package com.insighton.core.domain.actuators.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record ActuatorRequest(
        @NotBlank(message = "설치 구역 이름은 필수입니다")
        String locationName, // 설치 구역 이름 - 사용자는 locationId를 모르므로 이름으로 받음

        @NotBlank(message = "장비 이름 필수")
        @Size(max = 100, message = "장비 이름은 100자를 넘을 수 없습니다")
        String sensorName, // 장비 명칭

        @NotBlank(message = "액추에이터 타입 필수")
        @Size(max = 30, message = "액추에이터 타입은 30자를 넘을 수 없습니다")
        String actuatorType, // 액추에이터 종류

        @NotNull(message = "초기 상태값 필수")
        Map<String, Object> currentState // JSONB에 저장될 초기 상태 객체


) {
}
