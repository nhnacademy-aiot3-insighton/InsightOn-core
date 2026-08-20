package com.insighton.core.domain.gateway.event;

import com.insighton.core.common.config.RabbitConfig;
import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.entity.GatewayStatus;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendGatewayStatusChangedEvent(Gateway gateway) {
        try {
            GatewayStatusChangedEvent event = new GatewayStatusChangedEvent(
                    gateway.getGatewayId(),
                    gateway.getGroupId(),
                    gateway.getName(),
                    gateway.getStatus(),
                    OffsetDateTime.now()
            );

            rabbitTemplate.convertAndSend(
                    RabbitConfig.CORE_EVENTS_EXCHANGE,
                    RabbitConfig.GATEWAY_STATUS_ROUTING_KEY,
                    event
            );

            log.info("GatewayEventProducer 상태 변경 이벤트 전송 - gatewayId:{}, status: {}", gateway.getGatewayId(), gateway.getStatus());
        } catch (AmqpException e) {
            log.error("GatewayEventProducer 상태 변경 이벤트 전송 실패 - gatewayId:{}, error: {}", gateway.getGatewayId(), e.getMessage(), e);
        }
    }
}
