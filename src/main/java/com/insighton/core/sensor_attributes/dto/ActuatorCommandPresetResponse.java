package com.insighton.core.sensor_attributes.dto;

import java.util.Map;
import java.util.Set;

public record ActuatorCommandPresetResponse(
        Map<String, Set<String>> supportedCommandsByActuatorType // key: ActuatorType, value: 지원 CommandType 목록
) {}