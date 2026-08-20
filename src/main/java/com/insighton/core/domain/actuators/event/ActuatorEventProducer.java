package com.insighton.core.domain.actuators.event;

import com.insighton.core.common.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActuatorEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendActuatorDeleteEvent(Long actuatorId) {
        try {
            ActuatorDeletedEvent event = new ActuatorDeletedEvent(actuatorId);
            log.info("액추에이터 삭제 메세지 발행 시작 - Actuator ID : {}", actuatorId);

            rabbitTemplate.convertAndSend(
                    RabbitConfig.CORE_EVENTS_EXCHANGE,
                    RabbitConfig.ACTUATOR_DELETED_ROUTING_KEY,
                    event
            );

            log.info("[ActuatorEventProducer] 액추에이터 삭제 이벤트 전송 성공 - actuatorId: {}", actuatorId);
        } catch (AmqpException e) {
            log.error("[ActuatorEventProducer] 액추에이터 삭제 이벤트 전송 실패 - actuatorId: {}, error: {}",
                    actuatorId, e.getMessage(), e);
            throw e;
        }
    }
}
