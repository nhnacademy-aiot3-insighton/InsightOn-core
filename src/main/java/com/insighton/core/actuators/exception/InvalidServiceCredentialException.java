package com.insighton.core.actuators.exception;

public class InvalidServiceCredentialException extends RuntimeException {
    public InvalidServiceCredentialException(String message) {
        super(message);
    }
}