package com.insighton.core.domain.actuators.control;

import com.insighton.core.domain.actuators.entity.ActuatorType;
import java.util.Map;

// Facade가 Adapter에게 넘기는 공급자 독립 공통 제어 명령
public record ActuatorControlCommand(
        String externalDeviceId,
        ActuatorType actuatorType,
        Map<String, Object> desiredState
) {
}
