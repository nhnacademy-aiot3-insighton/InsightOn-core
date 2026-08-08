package com.insighton.core.domain.sensors.exception;

public class InvalidSensorValueException extends RuntimeException {
    public InvalidSensorValueException(String message) {
        super(message);
    }
}