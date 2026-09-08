package com.insighton.core.domain.groupmember.exception;

public class ManagerRoleRequiredForTransferException extends RuntimeException {
    public ManagerRoleRequiredForTransferException() {
        super("Manager 권한을 보유한 멤버에게만 Super Manager 권한을 양도할 수 있습니다.");
    }

    public ManagerRoleRequiredForTransferException(Long targetMemberId) {
        super(String.format("Member ID %d는 Super Manager 권한이 아니므로 Super Manager 권한을 양도할 수 없습니다.", targetMemberId));
    }
}
