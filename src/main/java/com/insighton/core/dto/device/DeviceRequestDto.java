package com.insighton.core.dto.device;


import jakarta.validation.constraints.NotBlank;

public record DeviceRequestDto(
        @NotBlank(message = "장치 이름 필수")
        String name,
        @NotBlank(message = "장치 타입 필수")
        String type,
        @NotBlank(message = "디바이스 EUI 필수")
        String deviceEui,
        @NotBlank(message = "게이트웨이 ID 필수")
        Long gatewayId,
        Long locationsId
) {
}