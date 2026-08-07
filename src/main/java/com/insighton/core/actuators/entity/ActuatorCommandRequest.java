package com.insighton.core.actuators.entity;

import com.insighton.core.actuator_run_logs.entity.ExecutedByType;

public record ActuatorCommandRequest(
        String actuatorType,
        String command,
        String commandValue,
        ExecutedByType callerService
) {
}