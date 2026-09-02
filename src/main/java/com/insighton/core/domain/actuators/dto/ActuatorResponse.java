package com.insighton.core.domain.actuators.dto;

import com.insighton.core.domain.actuators.control.ControlProvider;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.entity.Actuator;

import java.time.OffsetDateTime;
import java.util.Map;

public record ActuatorResponse(
        Long actuatorId, // 액추에이터 PK
        Long locationId, // 설치 구역 ID
        String sensorName, // 장비 이름
        ActuatorType actuatorType, // 액추에이터 종류
        Map<String, Object> currentState, // 현재 상태 JSON Map
        OffsetDateTime stateUpdatedAt, // 상태 변경 일시
        OffsetDateTime createdAt, // 생성 일시
        ControlProvider controlProvider, // 제어 공급자 (null이면 미연결)
        String externalDeviceId // 공급자 쪽 장치 식별자
) {

    public static ActuatorResponse from(Actuator entity){
        return new ActuatorResponse(
                entity.getActuatorId(),
                entity.getLocation().getLocationId(),
                entity.getSensorName(),
                entity.getActuatorType(),
                entity.getCurrentState(),
                entity.getStateUpdatedAt(),
                entity.getCreatedAt(),
                entity.getControlProvider(),
                entity.getExternalDeviceId()
        );
    }
}
