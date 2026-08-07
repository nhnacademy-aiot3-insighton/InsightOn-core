package com.insighton.core.sensors.exception;

public class SensorNotFoundException extends RuntimeException {
    public SensorNotFoundException(Long message) {
        super(("센서를 찾을 수 없습니다. (ID: " + message + ")"));
    }
}