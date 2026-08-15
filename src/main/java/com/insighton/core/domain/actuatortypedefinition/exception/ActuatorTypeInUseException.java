package com.insighton.core.domain.actuatortypedefinition.exception;

// 삭제하려는 종류를 실제로 쓰고 있는 액추에이터가 남아있을 때
public class ActuatorTypeInUseException extends RuntimeException {
    public ActuatorTypeInUseException(String typeCode, long usageCount) {
        super("사용 중인 액추에이터 종류는 삭제할 수 없습니다: " + typeCode + " (사용 중인 액추에이터 " + usageCount + "개)");
    }
}