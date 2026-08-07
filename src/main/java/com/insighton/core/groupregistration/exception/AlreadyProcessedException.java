package com.insighton.core.groupregistration.exception;


import com.insighton.core.groupregistration.entity.GroupRegistrationStatus;

public class AlreadyProcessedException extends RuntimeException {
    public AlreadyProcessedException(String message) {
        super(message);
    }

    public static AlreadyProcessedException of(GroupRegistrationStatus status) {
        return new AlreadyProcessedException("이미 %s 상태인 신청입니다.".formatted(status.getLabel()));
    }
}
