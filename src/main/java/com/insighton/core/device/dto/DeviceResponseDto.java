package com.insighton.core.device.dto;

import com.insighton.core.device.entity.DeviceType;
import java.time.ZonedDateTime;

public record DeviceResponseDto(
        Long deviceId,
        Long gatewaysId,
        Long locationsId,
        String deviceEui,
        String deviceName, // name -> deviceName
        DeviceType type,
        ZonedDateTime createdAt,
        ZonedDateTime lastSeenAt
) {
    public boolean isOnline(){
        // 5분전이내에 마지막 통신이 있다면 온라인
        ZonedDateTime now = ZonedDateTime.now();
        return lastSeenAt != null && !lastSeenAt.isBefore(now.minusMinutes(5));
    }
}