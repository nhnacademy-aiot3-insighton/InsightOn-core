package com.insighton.core.exception.groupmember;

public class AlreadyJoinedException extends RuntimeException {
    public AlreadyJoinedException(Long userId) {
        super("Already joined User ID : " + userId);
    }
}
