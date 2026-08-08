package com.insighton.core.domain.actuators.exception;

public class InvalidActuatorValueException extends RuntimeException {
    public InvalidActuatorValueException(String message) {
        super(message);
    }
}