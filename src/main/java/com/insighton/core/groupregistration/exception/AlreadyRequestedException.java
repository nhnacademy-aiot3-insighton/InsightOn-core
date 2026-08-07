package com.insighton.core.groupregistration.exception;

public class AlreadyRequestedException extends RuntimeException {
    public AlreadyRequestedException(String message) {
        super(message);
    }

    public static AlreadyRequestedException of() {
        return new AlreadyRequestedException("이미 처리 대기중인 그룹 생성 신청이 존재합니다.");
    }
}
