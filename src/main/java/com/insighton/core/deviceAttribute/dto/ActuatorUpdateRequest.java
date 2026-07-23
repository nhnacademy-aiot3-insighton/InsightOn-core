package com.insighton.core.deviceAttribute.dto;


import jakarta.validation.constraints.NotBlank;

// 엑추에이터 단일 메트릭 값 제어 변경 요청 DTO
public record ActuatorUpdateRequest (
    @NotBlank(message = "변경할 수치값은 필수")
    String value
){}
