package com.insighton.core.actuators.dto;

import com.insighton.core.actuators.entity.ActuatorType;
import lombok.Builder;

@Builder
public record ActuatorCreateServiceDto(
        Long locationId,
        String sensorName,
        ActuatorType actuatorType
) {
}