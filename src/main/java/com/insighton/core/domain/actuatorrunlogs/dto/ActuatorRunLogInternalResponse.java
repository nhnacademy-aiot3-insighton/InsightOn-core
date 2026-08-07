package com.insighton.core.domain.actuatorrunlogs.dto;

import com.insighton.core.domain.actuatorrunlogs.entity.ActuatorRunLog;

import java.time.OffsetDateTime;

// AI 리포트 배치 전용 응답 DTO - AI 팀 스펙(locationId/actuatorType 등 String)에 맞춰 별도로 분리
// 기존 ActuatorRunLogResponse(사용자용 실행이력 조회 API)와는 필드 구성이 달라 재사용하지 않음
public record ActuatorRunLogInternalResponse(
        Long locationId,
        Long actuatorId,
        String actuatorType,
        String commandType,
        String commandValue,
        String executedByType,
        OffsetDateTime executedAt
) {
    public static ActuatorRunLogInternalResponse from(ActuatorRunLog entity) {
        return new ActuatorRunLogInternalResponse(
                entity.getActuator().getLocation().getLocationId(),
                entity.getActuator().getActuatorId(),
                entity.getActuator().getActuatorType().name(),
                entity.getCommandType().name(),
                entity.getCommandValue(),
                entity.getExecutedByType().name(),
                entity.getExecutedAt()
        );
    }
}