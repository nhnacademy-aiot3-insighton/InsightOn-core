package com.insighton.core.actuators.dto;

import com.insighton.core.actuator_run_logs.entity.CommandType;
import com.insighton.core.actuator_run_logs.entity.CommandValueRule;
import com.insighton.core.actuators.entity.ActuatorType;

import java.util.Map;
import java.util.Set;

//  팀 확정 전 placeholder 값 - OPERATION_MODE 값 목록, SET_TEMPERATURE 범위는
// 실제 시뮬레이터/프론트 명세 확인 후 아래 내용만 교체하면 됨 (구조는 재사용)
public final class ActuatorCommandPreset {

    private ActuatorCommandPreset() {}

    private static final Map<ActuatorType, Map<CommandType, CommandValueRule>> RULES = Map.of(
            // 에어컨
            ActuatorType.AIRCON, Map.of(
                    CommandType.POWER_STATUS, new CommandValueRule.AllowedValues(Set.of("ON", "OFF")),
                    CommandType.OPERATION_MODE, new CommandValueRule.AllowedValues(Set.of("COOL", "DRY", "FAN", "AUTO")),
                    CommandType.SET_TEMPERATURE, new CommandValueRule.NumericRange(18, 30)
            ),
            // 공기 청정기
            ActuatorType.AIR_PURIFIER, Map.of(
                    CommandType.POWER_STATUS, new CommandValueRule.AllowedValues(Set.of("ON", "OFF")),
                    CommandType.OPERATION_MODE, new CommandValueRule.AllowedValues(Set.of("AUTO", "SLEEP", "TURBO"))
            ),
            // 환풍기
            ActuatorType.VENTILATION_FAN, Map.of(
                    CommandType.POWER_STATUS, new CommandValueRule.AllowedValues(Set.of("ON", "OFF")),
                    CommandType.OPERATION_MODE, new CommandValueRule.AllowedValues(Set.of("LOW", "MID", "HIGH"))
            )
    );

    // actuatorType이 어떤 명령들을 지원하는지 조회 - AI가 LLM 프롬프트에 이 타입은 이 명령만 가능 이라고 제약을 걸어둠
    public static Set<CommandType> getSupportedCommands(ActuatorType actuatorType) {
        // 정의 안 된 타입이면 빈 Map을 반환해서 NPE 없이 빈 Set으로 처리
        return RULES.getOrDefault(actuatorType, Map.of()).keySet();
    }

    // actuatorType + commandType 조합에서 이 값이 실제로 허용되는지 검증 - 저장/실행 직전 방어적 체크용
    public static boolean isValidValue(ActuatorType actuatorType, CommandType commandType, String value) {
        //구칙 자체가 없으면 null
        CommandValueRule rule = RULES.getOrDefault(actuatorType, Map.of()).get(commandType);
        // rule이 null이면 무족건 false, 있으면 그 규칙(고정값/범위)으로 실제 검증
        return rule != null && rule.isValid(value);
    }
}