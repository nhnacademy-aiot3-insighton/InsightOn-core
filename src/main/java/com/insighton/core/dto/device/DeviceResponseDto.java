package com.insighton.core.dto.device;

import java.time.ZonedDateTime;

public record DeviceResponseDto(
        Long deviceId,
        Long gatewaysId,
        Long locationsId,
        String deviceEui,
        String name,
        String type,
        ZonedDateTime createdAt,
        ZonedDateTime lastSeenAt
) {
    public boolean isOnline(){
        // 5분전이내에 마지막 통신이 있다면 온라인
        ZonedDateTime now = ZonedDateTime.now();
        return lastSeenAt != null && !lastSeenAt.isBefore(now.minusMinutes(5));
    }
}
