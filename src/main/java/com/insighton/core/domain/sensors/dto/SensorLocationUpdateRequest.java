package com.insighton.core.domain.sensors.dto;

import jakarta.validation.constraints.NotNull;

public record SensorLocationUpdateRequest(
        @NotNull(message = "변경할 위치 ID는 필수입니다.")
        Long locationId
) {}