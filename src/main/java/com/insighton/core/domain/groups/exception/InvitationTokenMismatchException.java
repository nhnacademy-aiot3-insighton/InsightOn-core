package com.insighton.core.domain.groups.exception;

public class InvitationTokenMismatchException extends RuntimeException {
    public InvitationTokenMismatchException() {
        super("초대 토큰이 일치하지 않습니다.");
    }
}
