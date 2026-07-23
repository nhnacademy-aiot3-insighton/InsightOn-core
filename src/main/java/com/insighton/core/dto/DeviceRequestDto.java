package com.insighton.core.dto;

import com.insighton.core.entity.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceRequestDto(
        @NotBlank(message = "장치 이름 필수")
        String deviceName, // name -> deviceName

        @NotNull(message = "장치 타입 필수")
        DeviceType type,

        @NotBlank(message = "디바이스 EUI 필수")
        String deviceEui,

        @NotNull(message = "게이트웨이 ID 필수")
        Long gatewayId,

        Long locationsId
) {
}