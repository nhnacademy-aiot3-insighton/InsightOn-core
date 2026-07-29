package com.insighton.core.groupmember.exception;

public class NotJoinedAnyGroupException extends RuntimeException {
    public NotJoinedAnyGroupException(Long userId) {
        super("아직 가입하지 않은 사용자 입니다. User ID : " + userId);
    }
}
