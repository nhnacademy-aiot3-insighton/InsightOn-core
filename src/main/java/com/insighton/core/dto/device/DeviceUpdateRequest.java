package com.insighton.core.dto.device;

/**
 * 장치 정보 수정 요청 시 사용하는 DTO Record.
 *
 * @param name 변경할 장치 명칭
 * @param locationId 이동 배치할 장소/구역 식별 ID
 */
public record DeviceUpdateRequest(
        String name,
        Long locationId
) {}