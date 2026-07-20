package com.insighton.core.groups.exception;

public class UnAuthorizedAccessException extends RuntimeException {
    public UnAuthorizedAccessException(Long userId) {
        super("Administrator privileges are required.");
    }
}
