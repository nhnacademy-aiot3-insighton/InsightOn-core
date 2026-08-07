package com.insighton.core.sensor_attributes.dto;

/**
 * 대시보드 위젯 및 클라이언트 프론트엔드 동기화용 장치 속성 응답 DTO Record.
 *
 * @param metricKey 수집/제어 대상 필드 식별자 (ex. "co2")
 * @param displayName Enum 기반 메트릭 한글 명칭 (ex. "이산화탄소")
 * @param unit Enum 기반 데이터 물리 정량 단위 (ex. "ppm", "°C", null)
 * @param currentValueStr 액추에이터 전용 최신 상태값 (ex. "ON", "24.0")
 */
public record SensorAttributeResponse(
        String metricKey,
        String displayName,
        String unit,
        String currentValueStr
) {}