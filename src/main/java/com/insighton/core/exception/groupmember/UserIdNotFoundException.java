package com.insighton.core.exception.groupmember;

public class UserIdNotFoundException extends RuntimeException {
    public UserIdNotFoundException(Long userId) {
        super("Not Found User ID : " + userId);
    }
}
