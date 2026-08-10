package com.insighton.core.domain.groupregistration.exception;

public class UnauthorizedGroupRegistrationAccessException extends RuntimeException {

    public UnauthorizedGroupRegistrationAccessException(Long requesterId) {
        super("본인의 신청 건만 취소할 수 있습니다. requesterId: %s".formatted(requesterId));
    }

    public UnauthorizedGroupRegistrationAccessException() {
        super("관리자만 처리할 수 있는 요청입니다.");
    }

    public static UnauthorizedGroupRegistrationAccessException forView() {
        return new UnauthorizedGroupRegistrationAccessException("본인의 신청 건 또는 관리자만 조회할 수 있습니다.");
    }

    private UnauthorizedGroupRegistrationAccessException(String message) {
        super(message);
    }
}
