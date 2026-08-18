package com.insighton.core.domain.location.event;

import com.insighton.core.common.config.RabbitConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationEventProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private LocationEventProducer locationEventProducer;

    @Test
    @DisplayName("sendLocationDeleteEvent 성공 - RabbitTemplate convertAndSend 호출")
    void sendLocationDeleteEvent_success() {
        // given
        Long locationId = 100L;

        // when
        locationEventProducer.sendLocationDeleteEvent(locationId);

        // then
        verify(rabbitTemplate, times(1)).convertAndSend(
                RabbitConfig.CORE_EVENTS_EXCHANGE,
                RabbitConfig.LOCATION_DELETED_ROUTING_KEY,
                new LocationDeletedEvent(locationId)
        );
    }

    @Test
    @DisplayName("sendLocationDeleteEvent 실패 - AmqpException 발생 시 예외 로깅 후 재던짐")
    void sendLocationDeleteEvent_fail_amqpException() {
        // given
        Long locationId = 100L;
        doThrow(new AmqpException("Connection refused") {})
                .when(rabbitTemplate).convertAndSend(
                        RabbitConfig.CORE_EVENTS_EXCHANGE,
                        RabbitConfig.LOCATION_DELETED_ROUTING_KEY,
                        new LocationDeletedEvent(locationId)
                );

        // when & then
        assertThatThrownBy(() -> locationEventProducer.sendLocationDeleteEvent(locationId))
                .isInstanceOf(AmqpException.class);
    }
}
