package com.insighton.core.domain.actuators.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 액추에이터 이름 수정 요청 DTO
public record ActuatorNameUpdateRequest(
        @NotBlank(message = "변경할 장비 이름은 필수입니다")
        @Size(max = 100, message = "장비 이름은 100자를 넘을 수 없습니다")
        String sensorName
) {
}
