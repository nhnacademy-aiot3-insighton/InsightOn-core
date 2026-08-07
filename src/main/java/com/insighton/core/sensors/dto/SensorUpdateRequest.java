package com.insighton.core.sensors.dto;

import jakarta.validation.constraints.Size;

public record SensorUpdateRequest(
        Long locationId,   // null이면 위치 유지
        @Size(max = 100, message = "장비 이름은 100자를 넘을 수 없습니다")
        String sensorName  // null이면 이름 유지
) {}