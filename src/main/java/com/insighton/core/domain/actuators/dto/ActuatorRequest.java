package com.insighton.core.domain.actuators.dto;


import com.insighton.core.domain.actuators.control.ControlProvider;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record ActuatorRequest(
        @NotNull(message = "설치 구역 ID는 필수입니다")
        Long locationId, // 설치 구역 ID

        @NotBlank(message = "장비 이름 필수")
        @Size(max = 100, message = "장비 이름은 100자를 넘을 수 없습니다")
        String sensorName, // 장비 명칭

        @NotNull(message = "액추에이터 타입 필수")
        ActuatorType actuatorType, // 액추에이터 종류

        @NotNull(message = "초기 상태값 필수")
        Map<String, Object> currentState, // JSONB에 저장될 초기 상태 객체

        ControlProvider controlProvider, // 선택 - 없으면 미연결(UNBOUND) 상태로 생성됨

        String externalDeviceId // 선택 - 공급자 쪽 장치 식별자

) {
}
