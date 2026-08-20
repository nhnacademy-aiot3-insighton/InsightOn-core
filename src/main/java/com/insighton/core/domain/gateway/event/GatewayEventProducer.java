package com.insighton.core.domain.gateway.event;

import com.insighton.core.common.config.RabbitConfig;
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

    public void sendGatewayStatusChangedEvent(GatewayStatusChangedEvent event) {
        try {
            log.info("게이트웨이 상태 변경 메세지 발행 시작 - gatewayId:{}, status: {}", event.gatewayId(), event.status());

            rabbitTemplate.convertAndSend(
                    RabbitConfig.CORE_EVENTS_EXCHANGE,
                    RabbitConfig.GATEWAY_STATUS_ROUTING_KEY,
                    event
            );

            log.info("[GatewayEventProducer] 상태 변경 이벤트 전송 성공 - gatewayId:{}, status: {}", event.gatewayId(), event.status());
        } catch (AmqpException e) {
            log.error("[GatewayEventProducer] 상태 변경 이벤트 전송 실패 - gatewayId:{}, error: {}", event.gatewayId(), e.getMessage(), e);
            throw e;
        }
    }
}
