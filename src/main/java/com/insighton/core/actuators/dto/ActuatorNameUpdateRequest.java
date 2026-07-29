package com.insighton.core.actuators.dto;

import jakarta.validation.constraints.NotBlank;

// 액추에이터 이름 수정 요청 DTO
public record ActuatorNameUpdateRequest(
        @NotBlank(message = "변경할 장비 이름은 필수입니다")
        String deviceName
) {
}
