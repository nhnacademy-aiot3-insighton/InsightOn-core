package com.insighton.core.sensors.event;

import com.insighton.core.common.config.RabbitConfig;
import com.insighton.core.domain.sensors.event.SensorDeletedEvent;
import com.insighton.core.domain.sensors.event.SensorEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensorEventProducerTest {

    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private SensorEventProducer sensorEventProducer;

    @Test
    @DisplayName("sendSensorDeleteEvent - 지정된 exchange/routingKey로 이벤트 발행")
    void 발행_성공() {
        sensorEventProducer.sendSensorDeleteEvent(1L);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.CORE_EVENTS_EXCHANGE),
                eq(RabbitConfig.SENSOR_DELETED_ROUTING_KEY),
                eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new SensorDeletedEvent(1L));
    }

    @Test
    @DisplayName("sendSensorDeleteEvent - AmqpException이 나면 그대로 다시 던짐 (재시도 트리거 조건)")
    void 발행_실패시_예외전파() {
        doThrow(new AmqpException("브로커 연결 실패") {})
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThrows(AmqpException.class, () -> sensorEventProducer.sendSensorDeleteEvent(1L));
    }
}
