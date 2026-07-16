package com.insighton.core.dto;

import java.time.ZonedDateTime;
import java.util.List;

public record DeviceResponseDto(
        Long deviceId,
        Long gatewaysId,
        Long locationsId,
        String deviceEui,
        String name,
        String type,
        ZonedDateTime createdAt
//        List<DeviceAttributeDto> attribteDtoList
) {
}
