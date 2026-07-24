package com.insighton.core.devices.dto;

//import com.insighton.core.device.entity.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceRequest(
        @NotBlank(message = "장치 이름 필수")
        String deviceName, // name -> deviceName

        @NotBlank(message = "디바이스 EUI 필수")
        String deviceEui,

        @NotNull(message = "게이트웨이 ID 필수")
        Long gatewayId,

        @NotNull(message = "위치 ID 필수")
        Long locationsId
) {
}