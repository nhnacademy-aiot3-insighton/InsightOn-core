package com.insighton.core.domain.sensors.event;

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
public class SensorEventListener {
    private final SensorEventProducer sensorEventProducer;

    // 커밋 후에만(롤백 시 미발행) 비동기로 발행 - 브로커 지연이 API 응답에 안 묻고, 실패해도 재시도로 커버
    @Async
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSensorDeleted(SensorDeletedEvent event) {

        log.info("[SensorEventListener] Sensor(id: {}) 삭제 이벤트 전송", event.sensorId());

        sensorEventProducer.sendSensorDeleteEvent(event.sensorId());
    }

    // 3번 재시도까지 전부 실패하면 여기로 떨어짐 - 더 이상 재시도하지 않고 로그만 남김 (완전한 전달 보장은 아닌 best-effort)
    @Recover
    public void recover(Exception e, SensorDeletedEvent event) {
        log.error("RabbitMQ 메시지 발송 최종 실패! Sensor ID: {}", event.sensorId(), e);
    }
}
