package com.insighton.core.adapter.mqtt.listener.exception;

public class UnsupportedMqttPayloadException extends RuntimeException {

    public UnsupportedMqttPayloadException(Class<?> payloadType) {
        super("지원하지 않는 페이로드 타입 " + payloadType);
    }
}
