package com.insighton.core.domain.groups.exception;

public class UnAuthorizedAccessException extends RuntimeException {
    public UnAuthorizedAccessException(Long userId) {
        super("Administrator privileges are required. User ID : " + userId);
    }
}
