package com.insighton.core.groupregistration.exception;

public class GroupRegistrationNotFoundException extends RuntimeException {
    public GroupRegistrationNotFoundException() {
        super("해당 그룹 등록 요청 정보를 찾을 수 없습니다.");
    }
}
