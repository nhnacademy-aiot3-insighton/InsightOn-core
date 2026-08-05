package com.insighton.core.sensors.exception;

public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(Long message) {
        super(("디바이스를 찾을 수 없습니다. (ID: " + message + ")"));
    }
}