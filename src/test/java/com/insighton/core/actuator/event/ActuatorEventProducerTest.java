package com.insighton.core.actuator.event;

import com.insighton.core.common.config.RabbitConfig;
import com.insighton.core.domain.actuators.event.ActuatorDeletedEvent;
import com.insighton.core.domain.actuators.event.ActuatorEventProducer;
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
class ActuatorEventProducerTest {

    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ActuatorEventProducer actuatorEventProducer;

    @Test
    @DisplayName("sendActuatorDeleteEvent - 지정된 exchange/routingKey로 이벤트 발행")
    void 발행_성공() {
        actuatorEventProducer.sendActuatorDeleteEvent(1L);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.CORE_EVENTS_EXCHANGE),
                eq(RabbitConfig.ACTUATOR_DELETED_ROUTING_KEY),
                eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new ActuatorDeletedEvent(1L));
    }

    @Test
    @DisplayName("sendActuatorDeleteEvent - AmqpException이 나면 그대로 다시 던짐 (재시도 트리거 조건)")
    void 발행_실패시_예외전파() {
        doThrow(new AmqpException("브로커 연결 실패") {})
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThrows(AmqpException.class, () -> actuatorEventProducer.sendActuatorDeleteEvent(1L));
    }
}
