package com.insighton.core.adapter.mqtt.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.insighton.core.adapter.mqtt.listener.dto.TelemetryEventMessage;
import com.insighton.core.common.config.RabbitConfig;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class TelemetryPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private RedisTemplate<String, TelemetryEventMessage> telemetryRedisTemplate;

    private TelemetryPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new TelemetryPublisher(rabbitTemplate, telemetryRedisTemplate);
    }

    private TelemetryEventMessage event() {
        return new TelemetryEventMessage(1L, 2L, 3L, Map.of("co2", 800), Instant.parse("2026-09-04T00:00:00Z"));
    }

    @Test
    void 정상_발행시_locationId를_해시헤더로_실어_RabbitMQ와_Redis_양쪽에_보낸다() {
        TelemetryEventMessage event = event();

        publisher.publish(event);

        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.TELEMETRY_EXCHANGE), eq(""), eq(event), captor.capture());

        org.springframework.amqp.core.Message message = new org.springframework.amqp.core.Message(new byte[0],
                new MessageProperties());
        captor.getValue().postProcessMessage(message);
        assertThat(message.getMessageProperties().getHeaders())
                .containsEntry(RabbitConfig.TELEMETRY_HASH_HEADER, "2");

        verify(telemetryRedisTemplate).convertAndSend(eq("telemetry:sensor:3"), eq(event));
    }

    @Test
    void RabbitMQ_발행이_실패해도_예외를_던지지_않고_Redis_발행은_계속된다() {
        willThrow(new AmqpException("연결 실패")).given(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(), any(MessagePostProcessor.class));

        assertThatCode(() -> publisher.publish(event())).doesNotThrowAnyException();

        verify(telemetryRedisTemplate).convertAndSend(eq("telemetry:sensor:3"), any());
    }

    @Test
    void Redis_발행이_실패해도_예외를_던지지_않는다() {
        given(telemetryRedisTemplate.convertAndSend(anyString(), any()))
                .willThrow(new RuntimeException("Redis 연결 실패"));

        assertThatCode(() -> publisher.publish(event())).doesNotThrowAnyException();
    }
}
