package com.insighton.core.sensors.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceNameUpdateRequest(
        @NotBlank(message = "변경할 장치 이름은 필수입니다.")
        String deviceName
) {}