package com.insighton.core.exception.groups;

public class UnAuthorizedAccessException extends RuntimeException {
    public UnAuthorizedAccessException(Long userId) {
        super("Administrator privileges are required.");
    }
}
