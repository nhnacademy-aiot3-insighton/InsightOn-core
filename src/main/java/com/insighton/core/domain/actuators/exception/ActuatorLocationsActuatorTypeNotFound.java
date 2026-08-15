package com.insighton.core.domain.actuators.exception;

public class ActuatorLocationsActuatorTypeNotFound extends RuntimeException {
    public ActuatorLocationsActuatorTypeNotFound(Long locationId, String actuatorType) { // 생성자 파라미터 enum -> String (Actuator.actuatorType 타입 변경에 맞춤)
        super(locationId + "위치에 " + actuatorType + "이 없습니다");
    }
}
