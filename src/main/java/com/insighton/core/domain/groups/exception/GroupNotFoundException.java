package com.insighton.core.domain.groups.exception;

public class GroupNotFoundException extends RuntimeException {
    public GroupNotFoundException(Long groupId) {
        super("Not Found Group. Group ID: " + groupId);
    }
}
