package com.insighton.core.dto.device;

import java.time.ZonedDateTime;

/**
 * 장치 기본 정보 단일/목록 조회 응답용 DTO Record.
 *
 * @param deviceId 장치 고유 식별 ID
 * @param gatewaysId 소속 게이트웨이 ID
 * @param locationsId 설치된 장소 ID
 * @param deviceEui 하드웨어 고유 식별 문자열 (DevEUI)
 * @param name 장치 명칭
 * @param type 장치 대분류 스펙 (SENSOR, AIRCON 등)
 * @param createdAt 장치 등록 일시
 * @param lastSeenAt 마지막 통신 수신 일시
 */
public record DeviceResponseDto(
        Long deviceId,
        Long gatewaysId,
        Long locationsId,
        String deviceEui,
        String name,
        String type,
        ZonedDateTime createdAt,
        ZonedDateTime lastSeenAt
) {
    /**
     * 최근 5분 이내 통신 이력이 존재하는지 검증하여 온라인 상태 여부를 반환합니다.
     *
     * @return 5분 이내 통신 신호가 수신되었으면 true (온라인), 그렇지 않으면 false (오프라인)
     */
    public boolean isOnline(){
        ZonedDateTime now = ZonedDateTime.now();
        return lastSeenAt != null && !lastSeenAt.isBefore(now.minusMinutes(5));
    }
}