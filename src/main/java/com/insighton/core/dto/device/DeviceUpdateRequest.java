package com.insighton.core.dto.device;

// 수정 요청 DTO
public record DeviceUpdateRequest(
        String name,
        Long locationId
) {}