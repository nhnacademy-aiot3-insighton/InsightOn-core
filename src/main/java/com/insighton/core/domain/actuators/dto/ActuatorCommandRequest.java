package com.insighton.core.domain.actuators.dto;

import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// location+actuatorType 기반 상태변경 API의 요청 바디 - AI/RuleEngine이 어떤 명령을 어떤 값으로 실행할지 담음
public record ActuatorCommandRequest(
        @NotBlank(message = "액추에이터 종류는 필수입니다")
        String actuatorType,  // 대상 액추에이터 종류 (ex. "AIRCON")
        @NotBlank(message = "실행 명령 키는 필수입니다")
        String command,       // 실행할 명령 키 (CommandType.stateKey와 매칭됨, ex. "power")
        @NotBlank(message = "명령에 적용할 값은 필수입니다")
        String commandValue,  // 명령에 적용할 값 (ex. "ON", "24.0")
        @NotNull(message = "호출 주체는 필수입니다")
        ExecutedByType callerService // 호출 주체 - USER는 이 내부 API에서 거부됨
) {
}