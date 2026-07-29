package com.insighton.core.mqtt.listener.dto;

import java.util.Map;

/**
 * ChirpStack 업링크 이벤트 원본 페이로드 매핑용.
 * devEui/deviceName이 최상위가 아니라 deviceInfo 안에 중첩돼 있는 실제 와이어 포맷을 그대로 반영함.
 * 이 레코드는 파싱 전용이며, 이후 로직은 {@link CleanTelemetryPacket}(정제된 형태)만 사용함.
 */
public record ChirpStackUplinkPacket(
    String time,
    DeviceInfo deviceInfo,
    Map<String, Object> object
) {
    public record DeviceInfo(String devEui, String deviceName) {}
}
