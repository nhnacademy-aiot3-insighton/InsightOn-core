package com.insighton.core.domain.gateway.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayEventListener {
    private final GatewayEventProducer gatewayEventProducer;

    @Async("gatewayEventExecutor")
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGatewayStatusChanged(GatewayStatusChangedEvent event) {

        log.info("[GatewayEventListener] Gateway(id: {}) 상태 변경 이벤트 전송 - status: {}", event.gatewayId(), event.status());

        gatewayEventProducer.sendGatewayStatusChangedEvent(event);
    }

    @Recover
    public void recover(Exception e, GatewayStatusChangedEvent event) {
        log.error("RabbitMQ 메시지 발송 최종 실패! Gateway ID: {}", event.gatewayId(), e);
    }
}
