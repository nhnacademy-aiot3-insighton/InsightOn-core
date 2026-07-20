package com.insighton.core.dto;

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
        return lastSeenAt != null && lastSeenAt.isAfter(ZonedDateTime.now().minusMinutes(5));
    }
}
