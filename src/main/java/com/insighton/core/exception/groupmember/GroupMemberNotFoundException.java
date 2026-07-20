package com.insighton.core.exception.groupmember;

public class GroupMemberNotFoundException extends RuntimeException {
    public GroupMemberNotFoundException(Long userId, Long groupId) {
        super("Not found Group Member. User ID: " + userId + "Group ID: " + groupId);
    }

    public GroupMemberNotFoundException(Long userId) {
        super("Not found Group Member. User ID: " + userId);
    }
}
