package com.insighton.core.domain.sensors.dto; // 센서 DTO 패키지 정의

import java.time.OffsetDateTime; // OffsetDateTime 임포트

// 센서 응답 표준 DTO Record
public record SensorResponse(
        Long sensorId,         // 장치 PK ID
        Long gatewayId,        // gatewayId (SENSOR 전용)
        Long locationId,       // locationId
        String sensorEui,      // 고유 시리얼 식별자 (SENSOR 전용)
        String sensorName,     // 장치 명칭
        OffsetDateTime createdAt, // 장치 등록 시각
        OffsetDateTime lastSeenAt // 마지막 통신(하트비트) 시각
) {
}