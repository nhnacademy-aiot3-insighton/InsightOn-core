package com.insighton.core.location.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationEventListener {
    private final LocationEventProducer locationEventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLocationDeleted(LocationDeletedEvent event) {

        log.info("[LocationEventListener] Location(id: {}) 삭제 이벤트 전송", event.locationId());

        locationEventProducer.sendLocationDeleteEvent(event.locationId());
    }
}
