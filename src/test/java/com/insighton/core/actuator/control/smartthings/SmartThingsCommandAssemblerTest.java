package com.insighton.core.actuator.control.smartthings;

import com.insighton.core.adapter.client.actuator.smartthings.SmartThingsApiException;
import com.insighton.core.adapter.client.actuator.smartthings.SmartThingsCommandAssembler;
import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandRequest;
import com.insighton.core.domain.actuators.control.ActuatorControlCommand;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmartThingsCommandAssemblerTest {

    private final SmartThingsCommandAssembler assembler = new SmartThingsCommandAssembler();

    private static ActuatorControlCommand aircon(Map<String, Object> state) {
        return new ActuatorControlCommand("st-aircon-001", ActuatorType.AIRCON, state);
    }

    @Test
    @DisplayName("power ON -> switch/on, power OFF -> switch/off")
    void power() {
        SmartThingsCommandRequest.Command on = assembler.assemble(aircon(Map.of("power", "ON"))).commands().get(0);
        assertThat(on.component()).isEqualTo("main");
        assertThat(on.capability()).isEqualTo("switch");
        assertThat(on.command()).isEqualTo("on");
        assertThat(on.arguments()).isEmpty();

        SmartThingsCommandRequest.Command off = assembler.assemble(aircon(Map.of("power", "OFF"))).commands().get(0);
        assertThat(off.command()).isEqualTo("off");
    }

    @Test
    @DisplayName("mode -> airConditionerMode/setAirConditionerMode, CORE 값이 SmartThings 값으로 매핑됨")
    void mode() {
        assertThat(assembler.assemble(aircon(Map.of("mode", "COOL"))).commands().get(0).arguments()).containsExactly("cool");
        assertThat(assembler.assemble(aircon(Map.of("mode", "DRY"))).commands().get(0).arguments()).containsExactly("dry");
        assertThat(assembler.assemble(aircon(Map.of("mode", "FAN"))).commands().get(0).arguments()).containsExactly("wind");
        assertThat(assembler.assemble(aircon(Map.of("mode", "AUTO"))).commands().get(0).arguments()).containsExactly("auto");

        SmartThingsCommandRequest.Command cmd = assembler.assemble(aircon(Map.of("mode", "COOL"))).commands().get(0);
        assertThat(cmd.capability()).isEqualTo("airConditionerMode");
        assertThat(cmd.command()).isEqualTo("setAirConditionerMode");
    }

    @Test
    @DisplayName("temperature -> thermostatCoolingSetpoint/setCoolingSetpoint, 정수는 int로 직렬화")
    void temperature() {
        SmartThingsCommandRequest.Command intTemp = assembler.assemble(aircon(Map.of("temperature", "24"))).commands().get(0);
        assertThat(intTemp.capability()).isEqualTo("thermostatCoolingSetpoint");
        assertThat(intTemp.command()).isEqualTo("setCoolingSetpoint");
        assertThat(intTemp.arguments()).containsExactly(24);

        SmartThingsCommandRequest.Command intFromNumber = assembler.assemble(aircon(Map.of("temperature", 26))).commands().get(0);
        assertThat(intFromNumber.arguments()).containsExactly(26);

        SmartThingsCommandRequest.Command decimal = assembler.assemble(aircon(Map.of("temperature", "24.5"))).commands().get(0);
        assertThat(decimal.arguments()).containsExactly(24.5);
    }

    @Test
    @DisplayName("전체 상태(power/mode/temperature)면 명령 3개가 조립됨")
    void 전체상태() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("power", "ON");
        state.put("mode", "COOL");
        state.put("temperature", "24");

        SmartThingsCommandRequest request = assembler.assemble(aircon(state));

        assertThat(request.commands()).hasSize(3);
        assertThat(request.commands()).extracting(SmartThingsCommandRequest.Command::capability)
                .containsExactlyInAnyOrder("switch", "airConditionerMode", "thermostatCoolingSetpoint");
    }

    @Test
    @DisplayName("변환할 명령이 하나도 없으면 SmartThingsApiException")
    void 빈상태() {
        assertThatThrownBy(() -> assembler.assemble(aircon(Map.of("unknownKey", "x"))))
                .isInstanceOf(SmartThingsApiException.class);
    }

    @Test
    @DisplayName("지원하지 않는 power/mode 값이면 SmartThingsApiException")
    void 비허용값() {
        assertThatThrownBy(() -> assembler.assemble(aircon(Map.of("power", "EXPLODE"))))
                .isInstanceOf(SmartThingsApiException.class);
        assertThatThrownBy(() -> assembler.assemble(aircon(Map.of("mode", "HYPERCOOL"))))
                .isInstanceOf(SmartThingsApiException.class);
    }

    @Test
    @DisplayName("AIR_PURIFIER mode -> airPurifierFanMode/setAirPurifierFanMode")
    void purifier_mode() {
        ActuatorControlCommand cmd = new ActuatorControlCommand(
                "st-purifier-001", ActuatorType.AIR_PURIFIER, Map.of("mode", "SLEEP"));

        SmartThingsCommandRequest.Command c = assembler.assemble(cmd).commands().get(0);

        assertThat(c.capability()).isEqualTo("airPurifierFanMode");
        assertThat(c.command()).isEqualTo("setAirPurifierFanMode");
        assertThat(c.arguments()).containsExactly("sleep");
    }

    @Test
    @DisplayName("windDirection FIXED/SWING -> fanOscillationMode/setFanOscillationMode (fixed/all)")
    void windDirection() {
        var fixed = assembler.assemble(aircon(Map.of("windDirection", "FIXED"))).commands().get(0);
        assertThat(fixed.capability()).isEqualTo("fanOscillationMode");
        assertThat(fixed.arguments()).containsExactly("fixed");
        assertThat(assembler.assemble(aircon(Map.of("windDirection", "SWING"))).commands().get(0).arguments())
                .containsExactly("all");
    }

    @Test
    @DisplayName("SmartThings 에어컨엔 AIRCLEAN 매핑이 없어 SmartThingsApiException")
    void aircon_airclean_미지원() {
        assertThatThrownBy(() -> assembler.assemble(aircon(Map.of("mode", "AIRCLEAN"))))
                .isInstanceOf(SmartThingsApiException.class);
    }

    @Test
    @DisplayName("VENTILATION_FAN mode -> fanSpeed/setFanSpeed (LOW/MID/HIGH -> 1/2/3 정수)")
    void fan_mode() {
        ActuatorControlCommand mid = new ActuatorControlCommand(
                "st-fan-001", ActuatorType.VENTILATION_FAN, Map.of("mode", "MID"));

        SmartThingsCommandRequest.Command c = assembler.assemble(mid).commands().get(0);

        assertThat(c.capability()).isEqualTo("fanSpeed");
        assertThat(c.command()).isEqualTo("setFanSpeed");
        assertThat(c.arguments()).containsExactly(2);
    }

    @Test
    @DisplayName("AIRCON이 아니어도 power(switch)는 장치 종류 무관하게 조립됨")
    void nonAircon_power_허용() {
        ActuatorControlCommand purifierPower = new ActuatorControlCommand(
                "st-purifier-001", ActuatorType.AIR_PURIFIER, Map.of("power", "ON"));

        SmartThingsCommandRequest request = assembler.assemble(purifierPower);

        assertThat(request.commands()).hasSize(1);
        assertThat(request.commands().get(0).capability()).isEqualTo("switch");
    }

    @Test
    @DisplayName("AIRCON이 아닌데 temperature를 보내면 거절 (temperature는 AIRCON만)")
    void nonAircon_temperature_거절() {
        ActuatorControlCommand cmd = new ActuatorControlCommand(
                "st-purifier-001", ActuatorType.AIR_PURIFIER, Map.of("temperature", 24));
        assertThatThrownBy(() -> assembler.assemble(cmd))
                .isInstanceOf(SmartThingsApiException.class);
    }
}
