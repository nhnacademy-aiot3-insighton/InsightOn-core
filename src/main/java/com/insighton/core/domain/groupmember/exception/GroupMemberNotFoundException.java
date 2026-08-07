package com.insighton.core.domain.groupmember.exception;

public class GroupMemberNotFoundException extends RuntimeException {

    private GroupMemberNotFoundException(String message) {
        super(message);
    }

    public static GroupMemberNotFoundException byUserIdAndGroupId(Long userId, Long groupId) {
        return new GroupMemberNotFoundException("Not found Group Member. User ID: " + userId + ", Group ID: " + groupId);
    }

    public static GroupMemberNotFoundException byMemberIdAndGroupId(Long groupMemberId, Long groupId) {
        return new GroupMemberNotFoundException("Not found Group Member. GroupMember ID: " + groupMemberId + ", Group ID: " + groupId);
    }

    public static GroupMemberNotFoundException byUserId(Long userId) {
        return new GroupMemberNotFoundException("Not found Group Member. User ID: " + userId);
    }
}