package com.insighton.core.exception.groups;

public class GroupNotFoundException extends RuntimeException {
    public GroupNotFoundException(Long groupId) {
        super("Not Found Group. Group ID: " + groupId);
    }
}
