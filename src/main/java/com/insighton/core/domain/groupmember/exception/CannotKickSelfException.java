package com.insighton.core.domain.groupmember.exception;

public class CannotKickSelfException extends RuntimeException {
    public CannotKickSelfException(Long groupMemberId) {
        super("자기 자신을 추방(강퇴)할 수 없습니다. (groupMemberId: " + groupMemberId + ")");
    }
}
