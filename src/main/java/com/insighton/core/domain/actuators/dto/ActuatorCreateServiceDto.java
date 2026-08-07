package com.insighton.core.domain.actuators.dto;

import com.insighton.core.domain.actuators.entity.ActuatorType;
import lombok.Builder;

@Builder
// 액추에이터 생성 시 컨트롤러 -> 서비스 계층으로 넘기는 내부 전용 DTO (요청 DTO와 분리해서 서비스 시그니처를 단순화)
public record ActuatorCreateServiceDto(
        Long locationId,
        String sensorName,
        ActuatorType actuatorType
) {
}