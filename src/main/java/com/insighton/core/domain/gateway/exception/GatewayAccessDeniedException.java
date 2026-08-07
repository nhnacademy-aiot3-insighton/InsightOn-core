package com.insighton.core.domain.gateway.exception;

public class GatewayAccessDeniedException extends RuntimeException {

    public GatewayAccessDeniedException(String message) {
        super(message);
    }
}
