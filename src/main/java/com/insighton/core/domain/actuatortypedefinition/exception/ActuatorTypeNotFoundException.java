package com.insighton.core.domain.actuatortypedefinition.exception;

// 존재하지 않는 타입 코드로 조회/삭제를 시도할 때
public class ActuatorTypeNotFoundException extends RuntimeException {
    public ActuatorTypeNotFoundException(String typeCode) {
        super("존재하지 않는 액추에이터 종류입니다: " + typeCode);
    }
}