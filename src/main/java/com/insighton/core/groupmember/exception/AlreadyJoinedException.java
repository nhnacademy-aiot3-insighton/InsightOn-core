package com.insighton.core.groupmember.exception;

public class AlreadyJoinedException extends RuntimeException {
    public AlreadyJoinedException(Long userId) {
        super("Already joined User ID : " + userId);
    }
}
