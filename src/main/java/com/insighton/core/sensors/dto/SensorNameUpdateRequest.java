package com.insighton.core.sensors.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SensorNameUpdateRequest(
        @NotBlank(message = "변경할 장치 이름은 필수입니다.")
        @Size(max = 100, message = "장비 이름은 100자를 넘을 수 없습니다")
        String sensorName
) {}