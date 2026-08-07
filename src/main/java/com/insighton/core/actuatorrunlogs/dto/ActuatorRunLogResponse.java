package com.insighton.core.actuatorrunlogs.dto;


import com.insighton.core.actuatorrunlogs.entity.ActuatorRunLog;
import com.insighton.core.actuatorrunlogs.entity.CommandType;
import com.insighton.core.actuatorrunlogs.entity.ExecutedByType;

import java.time.OffsetDateTime;

public record ActuatorRunLogResponse(
        Long runLogId, // 실행 로그 PK
        Long actuatorId, // 대상 액추에이터 ID
        CommandType commandType, // 제어 명령 종류
        String commandValue, // 제어 명령 값
        ExecutedByType executedByType, // 실행 주체
        Long executedByUserId, // 사용자가 실행한 경우 사용자 ID
        OffsetDateTime executedAt // 실행 시각
) {

    // ActuatorRunLogEntity를 응답 DTO로 변환
    public static ActuatorRunLogResponse from(ActuatorRunLog entity){
        return new ActuatorRunLogResponse(
                entity.getRunLogId(),
                entity.getActuator().getActuatorId(),
                entity.getCommandType(),
                entity.getCommandValue(),
                entity.getExecutedByType(),
                entity.getExecutedByUserId(),
                entity.getExecutedAt()
        );
    }
}
