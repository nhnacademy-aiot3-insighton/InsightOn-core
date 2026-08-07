package com.insighton.core.location.event;

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
public class LocationEventListener {
    private final LocationEventProducer locationEventProducer;

    @Async
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLocationDeleted(LocationDeletedEvent event) {

        log.info("[LocationEventListener] Location(id: {}) 삭제 이벤트 전송", event.locationId());

        locationEventProducer.sendLocationDeleteEvent(event.locationId());
    }

    @Recover
    public void recover(Exception e, LocationDeletedEvent event) {
        log.error("RabbitMQ 메시지 발송 최종 실패! Location ID: {}", event.locationId(), e);
    }
}
