package com.insighton.core.adapter.influx;

import com.insighton.core.domain.actuatorrunlogs.event.ActuatorStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ActuatorStatusEventListener {

    private final ActuatorStatusInfluxWriter actuatorStatusInfluxWriter;

    // PG 트랜잭션이 실제로 커밋된 뒤에만 실행됨 - 트랜잭션이 롤백되면 이 메서드 자체가 안 불려서
    // "PG엔 없는데 Influx엔 남아있는" 정합성 깨짐을 막아줌
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onActuatorStatusChanged(ActuatorStatusChangedEvent event) {
        actuatorStatusInfluxWriter.writeTransition(
                event.groupId(), event.locationId(), event.actuatorId(), event.actuatorType(),
                event.commandValue(), event.executedAt());
    }
}