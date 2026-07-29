package com.insighton.core.groupmember.exception;

public class UserIdNotFoundException extends RuntimeException {
    public UserIdNotFoundException(Long userId) {
        super("Not Found User ID : " + userId);
    }
}
