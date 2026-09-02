package com.insighton.core.actuator.control.lg;

import com.insighton.core.adapter.client.actuator.lg.LgThinQApiException;
import com.insighton.core.adapter.client.actuator.lg.LgThinQControlAssembler;
import com.insighton.core.adapter.client.actuator.lg.dto.LgThinQControlRequest;
import com.insighton.core.domain.actuators.control.ActuatorControlCommand;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LgThinQControlAssemblerTest {

    private final LgThinQControlAssembler assembler = new LgThinQControlAssembler();

    private static ActuatorControlCommand aircon(Map<String, Object> state) {
        return new ActuatorControlCommand("lg-aircon-001", ActuatorType.AIRCON, state);
    }

    @Test
    @DisplayName("power ON/OFF -> operation.airConOperationMode POWER_ON/POWER_OFF")
    void power() {
        assertThat(assembler.assemble(aircon(Map.of("power", "ON"))).operation().airConOperationMode())
                .isEqualTo("POWER_ON");
        assertThat(assembler.assemble(aircon(Map.of("power", "OFF"))).operation().airConOperationMode())
                .isEqualTo("POWER_OFF");
    }

    @Test
    @DisplayName("mode -> airConJobMode.currentJobMode (DRY -> AIR_DRY)")
    void mode() {
        assertThat(assembler.assemble(aircon(Map.of("mode", "COOL"))).airConJobMode().currentJobMode()).isEqualTo("COOL");
        assertThat(assembler.assemble(aircon(Map.of("mode", "DRY"))).airConJobMode().currentJobMode()).isEqualTo("AIR_DRY");
        assertThat(assembler.assemble(aircon(Map.of("mode", "FAN"))).airConJobMode().currentJobMode()).isEqualTo("FAN");
        assertThat(assembler.assemble(aircon(Map.of("mode", "AUTO"))).airConJobMode().currentJobMode()).isEqualTo("AUTO");
    }

    @Test
    @DisplayName("temperature -> temperature.targetTemperature (반올림 정수)")
    void temperature() {
        assertThat(assembler.assemble(aircon(Map.of("temperature", "24"))).temperature().targetTemperature()).isEqualTo(24);
        assertThat(assembler.assemble(aircon(Map.of("temperature", 26))).temperature().targetTemperature()).isEqualTo(26);
        assertThat(assembler.assemble(aircon(Map.of("temperature", "23.6"))).temperature().targetTemperature()).isEqualTo(24);
    }

    @Test
    @DisplayName("전체 상태면 operation/airConJobMode/temperature 세 그룹이 모두 채워짐")
    void 전체상태() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("power", "ON");
        state.put("mode", "COOL");
        state.put("temperature", "24");

        LgThinQControlRequest request = assembler.assemble(aircon(state));

        assertThat(request.operation()).isNotNull();
        assertThat(request.airConJobMode()).isNotNull();
        assertThat(request.temperature()).isNotNull();
    }

    @Test
    @DisplayName("변경 안 하는 property 그룹은 null (JSON에서 생략됨)")
    void 부분상태() {
        LgThinQControlRequest request = assembler.assemble(aircon(Map.of("power", "ON")));

        assertThat(request.operation()).isNotNull();
        assertThat(request.airConJobMode()).isNull();
        assertThat(request.temperature()).isNull();
    }

    @Test
    @DisplayName("변환할 명령이 없으면 LgThinQApiException")
    void 빈상태() {
        assertThatThrownBy(() -> assembler.assemble(aircon(Map.of("unknown", "x"))))
                .isInstanceOf(LgThinQApiException.class);
    }

    @Test
    @DisplayName("지원하지 않는 power/mode 값이면 LgThinQApiException")
    void 비허용값() {
        assertThatThrownBy(() -> assembler.assemble(aircon(Map.of("power", "EXPLODE"))))
                .isInstanceOf(LgThinQApiException.class);
        assertThatThrownBy(() -> assembler.assemble(aircon(Map.of("mode", "HYPERCOOL"))))
                .isInstanceOf(LgThinQApiException.class);
    }

    @Test
    @DisplayName("AIR_PURIFIER mode -> airPurifierJobMode.currentJobMode")
    void purifier_mode() {
        LgThinQControlRequest req = assembler.assemble(new ActuatorControlCommand(
                "lg-purifier-001", ActuatorType.AIR_PURIFIER, Map.of("mode", "SLEEP")));

        assertThat(req.airPurifierJobMode().currentJobMode()).isEqualTo("SLEEP");
        assertThat(req.airConJobMode()).isNull();
    }

    @Test
    @DisplayName("VENTILATION_FAN mode -> windStrength.windStrength")
    void fan_mode() {
        LgThinQControlRequest req = assembler.assemble(new ActuatorControlCommand(
                "lg-fan-001", ActuatorType.VENTILATION_FAN, Map.of("mode", "HIGH")));

        assertThat(req.windStrength().windStrength()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("AIRCON이 아닌데 temperature면 거절")
    void nonAircon_temperature_거절() {
        ActuatorControlCommand purifier = new ActuatorControlCommand(
                "lg-purifier-001", ActuatorType.AIR_PURIFIER, Map.of("temperature", 24));
        assertThatThrownBy(() -> assembler.assemble(purifier))
                .isInstanceOf(LgThinQApiException.class);
    }
}
