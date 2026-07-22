package com.insighton.core.groups.exception;


public class NoPermissionException extends RuntimeException {
    // 생성자는 private으로 막아서 외부에서 new로 아무 메시지나 넣는 것을 방지
    private NoPermissionException(String message) {
        super(message);
    }

    // 상황 1: 관리자 권한이 없을 때
    public static NoPermissionException forAdmin(Long groupMemberId) {
        return new NoPermissionException(
                String.format("[ID: %d] Administrator privileges are required.", groupMemberId)
        );
    }

    // 상황 2: 다른 그룹원/일반 데이터에 권한이 없을 때
    public static NoPermissionException forResource(Long groupMemberId) {
        return new NoPermissionException(
                String.format("[ID: %d] You do not have permission to access this resource.", groupMemberId)
        );
    }
}
