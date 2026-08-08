package com.insighton.core.domain.groupmember.exception;

public class SuperManagerCannotLeaveException extends RuntimeException {
    public SuperManagerCannotLeaveException(Long groupMemberId) {
        super("관리자 권한(SUPER_MANAGER)을 가진 유저(" + groupMemberId + ")는 그룹을 탈퇴할 수 없습니다. 권한 위임 후 탈퇴해주세요.");
    }
}
