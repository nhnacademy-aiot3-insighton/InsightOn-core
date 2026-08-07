package com.insighton.core.domain.actuators.exception;

public class InvalidServiceCredentialException extends RuntimeException {
    public InvalidServiceCredentialException(String message) {
        super(message);
    }
}