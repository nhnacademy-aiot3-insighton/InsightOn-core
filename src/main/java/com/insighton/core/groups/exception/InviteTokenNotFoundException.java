package com.insighton.core.groups.exception;

public class InviteTokenNotFoundException extends RuntimeException {
    public InviteTokenNotFoundException(String inviteToken) {
        super("not found invite token : " + inviteToken);
    }
}
