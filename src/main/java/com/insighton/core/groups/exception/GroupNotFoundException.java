package com.insighton.core.groups.exception;

public class GroupNotFoundException extends RuntimeException {
    public GroupNotFoundException(Long groupId) {
        super("Not Found Group. Group ID: " + groupId);
    }
}
