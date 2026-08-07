package com.insighton.core.domain.groupregistration.exception;

public class UnauthorizedGroupRegistrationAccessException extends RuntimeException {
    public UnauthorizedGroupRegistrationAccessException(Long requesterId) {
        super("본인의 신청 건만 취소할 수 있습니다. requesterId: %s".formatted(requesterId));
    }
}
