package com.insighton.core.actuators.exception;

public class ActuatorNotFoundException extends RuntimeException {
    public ActuatorNotFoundException(String message) {
        super(message);
    }
}

