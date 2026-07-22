package com.insighton.core.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 신규 장치 등록 API 요청 데이터를 바인딩하고 유효성을 검증하는 DTO Record.
 *
 * @param name 장치 이름 (필수)
 * @param type 장치 타입 (필수)
 * @param deviceEui 하드웨어 고유 식별 DevEUI (필수)
 * @param gatewayId 소속 게이트웨이 ID (필수)
 * @param locationsId 배치 구역 ID (선택)
 */
public record DeviceRequestDto(
        @NotBlank(message = "장치 이름 필수")
        String name,

        @NotBlank(message = "장치 타입 필수")
        String type,

        @NotBlank(message = "디바이스 EUI 필수")
        String deviceEui,

        @NotNull(message = "게이트웨이 ID 필수")
        Long gatewayId,

        Long locationsId
) {
}