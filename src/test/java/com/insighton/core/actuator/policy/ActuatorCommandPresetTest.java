package com.insighton.core.actuator.policy;

import com.insighton.core.domain.actuatorrunlogs.entity.CommandType;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.domain.actuators.policy.ActuatorCommandPreset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActuatorCommandPresetTest {

    @Test
    @DisplayName("getSupportedCommands - AIRCON은 전원/모드/온도 3개 명령 지원")
    void 지원커맨드_에어컨() {
        Set<CommandType> result = ActuatorCommandPreset.getSupportedCommands(ActuatorType.AIRCON);

        assertThat(result).containsExactlyInAnyOrder(
                CommandType.POWER_STATUS, CommandType.OPERATION_MODE,
                CommandType.WIND_DIRECTION, CommandType.SET_TEMPERATURE);
    }

    @Test
    @DisplayName("getSupportedCommands - AIR_PURIFIER는 SET_TEMPERATURE 미지원")
    void 지원커맨드_공기청정기_온도미지원() {
        Set<CommandType> result = ActuatorCommandPreset.getSupportedCommands(ActuatorType.AIR_PURIFIER);

        assertThat(result).doesNotContain(CommandType.SET_TEMPERATURE);
    }

    @Test
    @DisplayName("isValidValue - 허용된 고정값이면 true, 대소문자 무관")
    void 값검증_허용값_대소문자무관() {
        assertThat(ActuatorCommandPreset.isValidValue(ActuatorType.AIRCON, CommandType.POWER_STATUS, "ON")).isTrue();
        assertThat(ActuatorCommandPreset.isValidValue(ActuatorType.AIRCON, CommandType.POWER_STATUS, "on")).isTrue();
    }

    @Test
    @DisplayName("isValidValue - 허용 안 된 고정값이면 false")
    void 값검증_비허용값_false() {
        assertThat(ActuatorCommandPreset.isValidValue(ActuatorType.AIRCON, CommandType.POWER_STATUS, "PAUSE")).isFalse();
    }

    @Test
    @DisplayName("isValidValue - 숫자 범위 안이면 true, 밖이면 false")
    void 값검증_숫자범위() {
        assertThat(ActuatorCommandPreset.isValidValue(ActuatorType.AIRCON, CommandType.SET_TEMPERATURE, "25")).isTrue();
        assertThat(ActuatorCommandPreset.isValidValue(ActuatorType.AIRCON, CommandType.SET_TEMPERATURE, "35")).isFalse();
    }

    @Test
    @DisplayName("isValidValue - 타입은 있지만 그 커맨드 규칙이 없으면 false (rule=null)")
    void 값검증_규칙없는커맨드_false() {
        assertThat(ActuatorCommandPreset.isValidValue(ActuatorType.AIR_PURIFIER, CommandType.SET_TEMPERATURE, "25")).isFalse();
    }

    @Test
    @DisplayName("validateCommandValues - 전부 유효하면 예외 없음")
    void 일괄검증_성공() {
        assertThatNoException().isThrownBy(() ->
                ActuatorCommandPreset.validateCommandValues(ActuatorType.AIRCON, Map.of("power", "ON", "temperature", 24)));
    }

    @Test
    @DisplayName("validateCommandValues - 알 수 없는 커맨드 키면 InvalidActuatorValueException")
    void 일괄검증_모르는키_예외() {
        assertThatThrownBy(() ->
                ActuatorCommandPreset.validateCommandValues(ActuatorType.AIRCON, Map.of("unknown_key", "ON")))
                .isInstanceOf(InvalidActuatorValueException.class);
    }

    @Test
    @DisplayName("validateCommandValues - 값이 null이면 InvalidActuatorValueException")
    void 일괄검증_null값_예외() {
        Map<String, Object> newState = new HashMap<>();
        newState.put("power", null);

        assertThatThrownBy(() -> ActuatorCommandPreset.validateCommandValues(ActuatorType.AIRCON, newState))
                .isInstanceOf(InvalidActuatorValueException.class);
    }

    @Test
    @DisplayName("validateCommandValues - 허용 안 된 값이면 InvalidActuatorValueException")
    void 일괄검증_비허용값_예외() {
        assertThatThrownBy(() ->
                ActuatorCommandPreset.validateCommandValues(ActuatorType.AIRCON, Map.of("power", "EXPLODE")))
                .isInstanceOf(InvalidActuatorValueException.class);
    }
}
