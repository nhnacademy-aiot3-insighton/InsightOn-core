package com.insighton.core.domain.groupregistration.exception;


import com.insighton.core.domain.groupregistration.entity.GroupRegistrationStatus;

public class AlreadyProcessedException extends RuntimeException {
    public AlreadyProcessedException(String message) {
        super(message);
    }

    public static AlreadyProcessedException of(GroupRegistrationStatus status) {
        return new AlreadyProcessedException("이미 %s 상태인 신청입니다.".formatted(status.getLabel()));
    }

    public static AlreadyProcessedException of() {
        return new AlreadyProcessedException("이미 다른 관리자가 처리한 신청입니다.");
    }
}
