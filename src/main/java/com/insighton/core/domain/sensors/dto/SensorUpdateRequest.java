package com.insighton.core.domain.sensors.dto;

import jakarta.validation.constraints.Size;

public record SensorUpdateRequest(
        String locationName,   // null이면 위치 유지 - 사용자는 locationId를 모르므로 이름으로 받음
        @Size(max = 100, message = "장비 이름은 100자를 넘을 수 없습니다")
        String sensorName  // null이면 이름 유지
) {}