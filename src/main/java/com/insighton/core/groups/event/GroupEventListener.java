package com.insighton.core.groups.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupEventListener {
    private final GroupEventProducer groupEventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupDeleted(GroupDeletedEvent event) {

        log.info("[GroupEventListener] Group(id: {}) 삭제 이벤트 전송", event.groupId());

        groupEventProducer.sendGroupDeleteEvent(event.groupId(), event.locationIds());
    }
}
