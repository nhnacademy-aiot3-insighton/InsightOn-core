package com.insighton.core.sensors.dto; // 디바이스 DTO 패키지 정의

import jakarta.validation.constraints.NotBlank; // NotBlank 검증 어노테이션 임포트

// ACTUATOR 장치 수동 등록 전용 요청 DTO Record
public record DeviceRequest(
        @NotBlank(message = "장치 이름은 필수입니다.") // 필수 값 검증
        String deviceName, // 생성할 장치 이름

        Long locationId // [수정] locationsId -> locationId 단수형으로 일관성 통일
) {}