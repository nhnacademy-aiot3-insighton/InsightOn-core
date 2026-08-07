package com.insighton.core.sensor_attributes.exception;

public class MetricKeyAlreadyExistsException extends RuntimeException {
    public MetricKeyAlreadyExistsException(String metricKey) {
        super("이미 등록된 메트릭 키입니다. (metricKey: " + metricKey + ")");
    }
}