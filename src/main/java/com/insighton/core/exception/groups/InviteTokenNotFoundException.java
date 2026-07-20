package com.insighton.core.exception.groups;

public class InviteTokenNotFoundException extends RuntimeException {
    public InviteTokenNotFoundException(String inviteToken) {
        super("not found invite token : " + inviteToken);
    }
}
