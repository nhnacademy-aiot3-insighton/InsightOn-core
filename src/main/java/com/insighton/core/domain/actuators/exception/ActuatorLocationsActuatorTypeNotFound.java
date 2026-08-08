package com.insighton.core.domain.actuators.exception;

import com.insighton.core.domain.actuators.entity.ActuatorType;

public class ActuatorLocationsActuatorTypeNotFound extends RuntimeException {
    public ActuatorLocationsActuatorTypeNotFound(Long locationId, ActuatorType actuatorType) {
        super(locationId + "위치에 " + actuatorType + "이 없습니다");
    }
}
