package com.insighton.core.domain.groups.event;

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
public class GroupEventListener {
    private final GroupEventProducer groupEventProducer;

    @Async
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupDeleted(GroupDeletedEvent event) {

        log.info("[GroupEventListener] Group(id: {}) 삭제 이벤트 전송", event.groupId());

        groupEventProducer.sendGroupDeleteEvent(event.groupId(), event.locationIds());
    }

    @Recover
    public void recover(Exception e, GroupDeletedEvent event) {
        log.error("RabbitMQ 메시지 발송 최종 실패! Group ID : {}", event.groupId(), e);
    }
}
