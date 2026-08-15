package com.insighton.core.domain.actuatortypedefinition.exception;

// 이미 등록된 타입 코드로 액추에이터 종류를 생성하려 할 때
public class ActuatorTypeAlreadyExistsException extends RuntimeException {
    public ActuatorTypeAlreadyExistsException(String typeCode) {
        super("이미 존재하는 액추에이터 종류입니다: " + typeCode);
    }
}
