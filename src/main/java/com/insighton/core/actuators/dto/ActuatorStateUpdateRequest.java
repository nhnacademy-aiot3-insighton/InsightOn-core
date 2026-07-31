package com.insighton.core.actuators.dto;


import jakarta.validation.constraints.NotNull;
import java.util.Map;

// 액추에이터 제어 명령(상태 변경) 요청 DTO
public record ActuatorStateUpdateRequest(
        @NotNull(message = "변경할 상태값은 필수입니다")
        Map<String, Object> currentState
) {
}
