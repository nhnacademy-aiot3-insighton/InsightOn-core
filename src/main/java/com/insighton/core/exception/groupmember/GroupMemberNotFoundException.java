package com.insighton.core.exception.groupmember;

public class GroupMemberNotFoundException extends RuntimeException {
    public GroupMemberNotFoundException(Long userId, Long groupId) {
        super("Not found ");
    }
}
