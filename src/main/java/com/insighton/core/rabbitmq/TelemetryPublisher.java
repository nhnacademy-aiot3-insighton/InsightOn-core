package com.insighton.core.rabbitmq;

import com.insighton.core.config.RabbitConfig;
import com.insighton.core.rabbitmq.dto.TelemetryEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 정제된 Telemetry DTO Rule Engine으로 전파용
 * RabbitMQ 장애가 MQTT 리스너 스레드 막지 않도록 비동기 처리 + Fail-Silent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Async("telemetryDispatchExecutor")
    public void publish(TelemetryEventMessage event) {
        String routingKey = RabbitConfig.TELEMETRY_ROUTING_KEY_PREFIX + event.groupsId();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.TELEMETRY_EXCHANGE,
                    routingKey,
                    event
            );
        } catch (AmqpException e) {
            log.warn("RabbitMQ 발행 실패, 메시지 드롭 (groupId = {} , locationId = {})", event.groupsId(), event.locationsId(), e);
        }
    }
}
