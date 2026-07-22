package com.insighton.core.groups.exception;

public class InviteTokenNotFoundException extends RuntimeException {
    public InviteTokenNotFoundException() {
        super("not found invite token");
    }
}
