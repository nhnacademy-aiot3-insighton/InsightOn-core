package com.insighton.core.gateway.exception;

public class GatewayAccessDeniedException extends RuntimeException {

    public GatewayAccessDeniedException(String message) {
        super(message);
    }
}
