package com.insighton.core.sensors.entity; // 디바이스 엔티티 패키지 정의

// IoT 기기의 유형(센서 / 액추에이터)을 구분하는 Enum 열거형 클래스
public enum DeviceType {
    SENSOR,   // 데이터 수집 전용 센서 장치 (MQTT 패킷 수신 시 Auto-Provisioning으로 자동 생성)
    ACTUATOR  // 제어 명령 수신 전용 액추에이터 장치 (공개 REST API를 통해 수동 등록)
}