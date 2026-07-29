package com.insighton.core.actuators.dto;


import com.insighton.core.actuators.entity.ActuatorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ActuatorsRequest(
        @NotNull(message = "설치 구역 ID는 필수입니다")
        Long locationId, // 설치 구역 PK

        @NotBlank(message = "장비 이름 필수")
        String deviceName, // 장비 명칭

        @NotNull(message = "액추에이터 타입 필수")
        ActuatorType actuatorType, // 액추에이터 종류

        @NotNull(message = "초기 상태값 필수")
        Map<String, Object> currentState // JSONB에 저장될 초기 상태 객체


) {
}
