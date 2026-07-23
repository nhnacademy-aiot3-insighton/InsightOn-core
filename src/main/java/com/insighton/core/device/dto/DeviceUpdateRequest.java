package com.insighton.core.device.dto;

// 수정 요청 DTO
public record DeviceUpdateRequest(
        String deviceName, // name -> deviceName
        Long locationId
) {}