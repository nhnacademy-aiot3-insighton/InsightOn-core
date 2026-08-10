package com.insighton.core.domain.groupmember.exception;

public class UserIdNotFoundException extends RuntimeException {
    public UserIdNotFoundException(Long userId) {
        super("Not Found User ID : " + userId);
    }
}
