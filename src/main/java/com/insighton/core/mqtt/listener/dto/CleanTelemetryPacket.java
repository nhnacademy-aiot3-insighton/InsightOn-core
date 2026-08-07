package com.insighton.core.mqtt.listener.dto;

import java.util.Map;

/**
 * 정제된 MQTT 패킷 데이터를 담는 불변 객체
 */
public record CleanTelemetryPacket(
    String time,
    String sensorEui,
    String sensorName,
    Map<String, Object> object
) {}
