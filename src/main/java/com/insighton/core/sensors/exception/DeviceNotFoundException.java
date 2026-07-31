package com.insighton.core.sensors.exception;

public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(String message) {
        super(message);
    }
}