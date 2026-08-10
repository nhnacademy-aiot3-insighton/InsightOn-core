package com.insighton.core.adapter.mqtt.listener.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * ChirpStack 업링크 이벤트 원본 페이로드 매핑용.
 * ChirpStack은 기기를 "device"라고 부르지만 우리 도메인 용어는 "sensor"라, 필드명은 내부 용어로
 * 맞추고 실제 JSON 키는 {@code @JsonProperty}로 고정함 — 이 매핑을 지우면 파싱이 조용히 실패하므로
 * ChirpStack 쪽 스펙이 바뀌지 않는 한 애노테이션 값은 건드리면 안 됨.
 * 이 레코드는 파싱 전용이며, 이후 로직은 {@link CleanTelemetryPacket}(정제된 형태)만 사용함.
 */
public record ChirpStackUplinkPacket(
    String time,

    // ChirpStack은 devEui/deviceName을 최상위가 아니라 deviceInfo 안에 중첩해서 보냄
    @JsonProperty("deviceInfo") SensorInfo sensorInfo,

    Map<String, Object> object
) {
    public record SensorInfo(
            @JsonProperty("devEui") String sensorEui,
            @JsonProperty("deviceName") String sensorName
    ) {}
}
