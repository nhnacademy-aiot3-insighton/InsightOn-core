package com.insighton.core.domain.gateway.exception;

public class GatewayAlreadyExistsException extends RuntimeException {

    public GatewayAlreadyExistsException(String message) {
        super(message);
    }
}
