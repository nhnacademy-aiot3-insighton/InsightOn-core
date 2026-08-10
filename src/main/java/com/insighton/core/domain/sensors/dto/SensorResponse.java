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
        OffsetDateTime lastSeenAt // 최근 통신 수신 시각 (SENSOR 전용)
) {
    // 최근 5분 이내 통신 이력이 있으면 온라인으로 판단하는 도메인 로직 메서드
    public boolean isOnline(){
        // 현재 시각 구하기
        OffsetDateTime now = OffsetDateTime.now();
        // lastSeenAt 시각이 존재하고 5분 전보다 이후 시각이면 true 반환
        return lastSeenAt != null && !lastSeenAt.isBefore(now.minusMinutes(5));
    }
}