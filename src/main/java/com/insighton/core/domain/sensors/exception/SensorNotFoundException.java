package com.insighton.core.domain.sensors.exception;

public class SensorNotFoundException extends RuntimeException {
    public SensorNotFoundException(Long message) {
        super(("센서를 찾을 수 없습니다. (ID: " + message + ")"));
    }
}