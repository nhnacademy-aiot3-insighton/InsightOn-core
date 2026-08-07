package com.insighton.core.actuatorrunlogs.event;

import java.time.OffsetDateTime;

// recordRunLogs
// 액추에이터 POWER_STATUS 전환 이벤트 - Influx 기록용 (트랜잭션 커밋 후 소비됨)
public record ActuatorStatusChangedEvent(
        String groupId,
        String locationId,
        String actuatorId,
        String actuatorType,
        String commandValue,
        OffsetDateTime executedAt
) {}