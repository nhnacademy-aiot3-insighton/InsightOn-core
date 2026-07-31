package com.insighton.core.sensors.entity; // 디바이스 엔티티 패키지 정의

// MQTT 수신 패킷 처리 속도 최적화를 위해 인메모리 캐시에 저장할 경량 디바이스 데이터 Record
public record DeviceCacheEntry(
        Long deviceId,   // 디바이스 데이터베이스 PK 식별자
        String deviceEui,// 센서 하드웨어 고유 식별자 (DevEUI)
        Long gatewayId,  // 센서가 연결된 상위 게이트웨이 ID
        Long locationId  // 센서가 설치된 장소/위치 ID
) {}