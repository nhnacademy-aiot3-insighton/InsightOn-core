package com.insighton.core.domain.actuatortypedefinition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 액추에이터 종류 생성 요청 DTO
public record ActuatorTypeCreateRequest (

        @NotBlank(message = "타입 코드는 필수입니다")
        @Size(max = 30, message = "타입 코드는 30자를 넘을 수 없습니다")
        String typeCode, // Actuator.actuatorType에 저장될 값 - 영문 대문자 코드 (예: AIRCON)

        @NotBlank(message = "타입 이름은 필수입니다")
        @Size(max = 50, message = "타입 이름은 50자를 넘을 수 없습니다")
        String typeName, // 화면에 보여줄 이름 (예: 에어컨)

        @Size(max = 100, message = "설명은 100자를 넘을 수 없습니다")
        String description // 종류 설명 (선택)
){
}
