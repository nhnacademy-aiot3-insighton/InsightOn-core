package com.insighton.core.actuators.exception;

public class ActuatorNotFoundException extends RuntimeException {
    public ActuatorNotFoundException(Long message) {
        super("액추에이터를 찾을 수 없습니다. (ID: " + message + ")");
    }
}

