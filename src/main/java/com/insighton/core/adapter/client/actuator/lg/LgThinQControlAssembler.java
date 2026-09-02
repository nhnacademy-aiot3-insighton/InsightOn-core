package com.insighton.core.adapter.client.actuator.lg;

import com.insighton.core.adapter.client.actuator.lg.dto.LgThinQControlRequest;
import com.insighton.core.domain.actuators.control.ActuatorControlCommand;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import org.springframework.stereotype.Component;

import java.util.Map;

// 공급자 독립 공통 명령(ActuatorControlCommand) -> LG ThinQ operation payload.
// 지원 범위(플랜 §14): AIRCON power/mode/temperature, AIR_PURIFIER/VENTILATION_FAN power/mode.
@Component
public class LgThinQControlAssembler {

    // AIRCON: CORE OPERATION_MODE -> LG currentJobMode
    private static final Map<String, String> AIRCON_JOB_MODE = Map.of(
            "COOL", "COOL", "DRY", "AIR_DRY", "FAN", "FAN", "AUTO", "AUTO");

    // AIR_PURIFIER: CORE mode -> LG airPurifierJobMode.currentJobMode
    private static final Map<String, String> PURIFIER_JOB_MODE = Map.of(
            "AUTO", "AUTO", "SLEEP", "SLEEP", "TURBO", "TURBO");

    // VENTILATION_FAN: CORE mode -> LG windStrength
    private static final Map<String, String> WIND_STRENGTH = Map.of(
            "LOW", "LOW", "MID", "MID", "HIGH", "HIGH");

    public LgThinQControlRequest assemble(ActuatorControlCommand command) {
        ActuatorType type = command.actuatorType();
        Map<String, Object> state = command.desiredState();

        LgThinQControlRequest.Operation operation = null;
        LgThinQControlRequest.AirConJobMode airConJobMode = null;
        LgThinQControlRequest.Temperature temperature = null;
        LgThinQControlRequest.AirPurifierJobMode airPurifierJobMode = null;
        LgThinQControlRequest.WindStrength windStrength = null;

        if (state.containsKey("power")) {
            operation = new LgThinQControlRequest.Operation(toOperationMode(state.get("power")));
        }
        if (state.containsKey("mode")) {
            String key = String.valueOf(state.get("mode")).toUpperCase();
            switch (type) {
                case AIRCON -> airConJobMode = new LgThinQControlRequest.AirConJobMode(
                        lookup(AIRCON_JOB_MODE, key));
                case AIR_PURIFIER -> airPurifierJobMode = new LgThinQControlRequest.AirPurifierJobMode(
                        lookup(PURIFIER_JOB_MODE, key));
                case VENTILATION_FAN -> windStrength = new LgThinQControlRequest.WindStrength(
                        lookup(WIND_STRENGTH, key));
            }
        }
        if (state.containsKey("temperature")) {
            if (type != ActuatorType.AIRCON) {
                throw new LgThinQApiException(
                        "LG ThinQ 어댑터는 temperature를 AIRCON에서만 지원합니다 (actuatorType=" + type + ")");
            }
            temperature = new LgThinQControlRequest.Temperature(toTargetTemperature(state.get("temperature")));
        }

        if (operation == null && airConJobMode == null && temperature == null
                && airPurifierJobMode == null && windStrength == null) {
            throw new LgThinQApiException("LG ThinQ로 변환할 수 있는 명령이 없습니다: " + state);
        }
        return new LgThinQControlRequest(operation, airConJobMode, temperature, airPurifierJobMode, windStrength);
    }

    private String toOperationMode(Object power) {
        return switch (String.valueOf(power).toUpperCase()) {
            case "ON" -> "POWER_ON";
            case "OFF" -> "POWER_OFF";
            default -> throw new LgThinQApiException("지원하지 않는 power 값입니다: " + power);
        };
    }

    private String lookup(Map<String, String> table, String key) {
        String v = table.get(key);
        if (v == null) {
            throw new LgThinQApiException("지원하지 않는 mode 값입니다: " + key);
        }
        return v;
    }

    private Integer toTargetTemperature(Object temperature) {
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(temperature)));
        } catch (NumberFormatException e) {
            throw new LgThinQApiException("temperature는 숫자여야 합니다: " + temperature);
        }
    }
}
