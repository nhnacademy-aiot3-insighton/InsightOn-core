package com.insighton.core.actuators.exception;

import com.insighton.core.actuators.entity.ActuatorType;

public class ActuatorLocationsActuatorTypeNotFound extends RuntimeException {
    public ActuatorLocationsActuatorTypeNotFound(Long locationId, ActuatorType actuatorType) {
        super(locationId + "위치에 " + actuatorType + "이 없습니다");
    }
}
