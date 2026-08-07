package com.insighton.core.sensor_attributes.exception;

public class MetricKeyNotFoundException extends RuntimeException {
    public MetricKeyNotFoundException(String message) {
        super("메트릭 키를 찾을 수 없습니다. (metricKey: " + message + ")");
    }
}
