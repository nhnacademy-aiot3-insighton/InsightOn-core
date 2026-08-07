package com.insighton.core.actuators.dto;

import com.insighton.core.actuatorrunlogs.entity.ExecutedByType;

// location+actuatorType 기반 상태변경 API의 요청 바디 - AI/RuleEngine이 어떤 명령을 어떤 값으로 실행할지 담음
public record ActuatorCommandRequest(
        String actuatorType,  // 대상 액추에이터 종류 (ex. "AIRCON")
        String command,       // 실행할 명령 키 (CommandType.stateKey와 매칭됨, ex. "power")
        String commandValue,  // 명령에 적용할 값 (ex. "ON", "24.0")
        ExecutedByType callerService // 호출 주체 - USER는 이 내부 API에서 거부됨
) {
}