package com.insighton.core.sensors.dto;

import jakarta.validation.constraints.NotNull;

public record DeviceLocationUpdateRequest(
        @NotNull(message = "변경할 위치 ID는 필수입니다.")
        Long locationId
) {}