package com.insighton.core.domain.gateway.event;

import com.insighton.core.domain.gateway.entity.GatewayStatus;
import java.time.OffsetDateTime;

public record GatewayStatusChangedEvent(
        Long gatewayId,
        Long groupId,
        String gatewayName,
        GatewayStatus status,
        OffsetDateTime occurredAt
) {
}
