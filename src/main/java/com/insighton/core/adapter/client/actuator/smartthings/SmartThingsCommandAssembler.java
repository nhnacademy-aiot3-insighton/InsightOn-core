package com.insighton.core.adapter.client.actuator.smartthings;

import com.insighton.core.adapter.client.actuator.smartthings.dto.SmartThingsCommandRequest;
import com.insighton.core.domain.actuators.control.ActuatorControlCommand;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 공급자 독립 공통 명령(ActuatorControlCommand) -> SmartThings capability 명령(SmartThingsCommandRequest).
// 지원 범위(플랜 §14): AIRCON power/mode/temperature, AIR_PURIFIER/VENTILATION_FAN power/mode.
// 장치 타입별 capability는 실제 장치 모델마다 다를 수 있어 공식형 근사 매핑 subset만 구현.
@Component
public class SmartThingsCommandAssembler {

    private static final String COMPONENT_MAIN = "main";

    // AIRCON: CORE OPERATION_MODE -> SmartThings airConditionerMode
    private static final Map<String, String> AIRCON_MODE = Map.of(
            "COOL", "cool", "DRY", "dry", "FAN", "wind", "AUTO", "auto");

    // AIR_PURIFIER: CORE mode -> SmartThings airPurifierFanMode
    private static final Map<String, String> PURIFIER_MODE = Map.of(
            "AUTO", "auto", "SLEEP", "sleep", "TURBO", "turbo");

    // VENTILATION_FAN: CORE mode -> SmartThings fanSpeed(정수)
    private static final Map<String, Integer> FAN_SPEED = Map.of(
            "LOW", 1, "MID", 2, "HIGH", 3);

    public SmartThingsCommandRequest assemble(ActuatorControlCommand command) {
        Map<String, Object> state = command.desiredState();
        List<SmartThingsCommandRequest.Command> commands = new ArrayList<>();

        if (state.containsKey("power")) {
            commands.add(switchCommand(state.get("power")));
        }
        if (state.containsKey("mode")) {
            commands.add(modeCommand(command.actuatorType(), state.get("mode")));
        }
        if (state.containsKey("temperature")) {
            requireAircon(command.actuatorType(), "temperature");
            commands.add(coolingSetpointCommand(state.get("temperature")));
        }

        if (commands.isEmpty()) {
            throw new SmartThingsApiException("SmartThings로 변환할 수 있는 명령이 없습니다: " + state);
        }
        return new SmartThingsCommandRequest(commands);
    }

    private SmartThingsCommandRequest.Command switchCommand(Object power) {
        String value = String.valueOf(power);
        String stCommand = switch (value.toUpperCase()) {
            case "ON" -> "on";
            case "OFF" -> "off";
            default -> throw new SmartThingsApiException("지원하지 않는 power 값입니다: " + value);
        };
        return new SmartThingsCommandRequest.Command(COMPONENT_MAIN, "switch", stCommand, List.of());
    }

    private SmartThingsCommandRequest.Command modeCommand(ActuatorType actuatorType, Object mode) {
        String key = String.valueOf(mode).toUpperCase();
        return switch (actuatorType) {
            case AIRCON -> capabilityCommand("airConditionerMode", "setAirConditionerMode",
                    lookup(AIRCON_MODE, key, "mode"));
            case AIR_PURIFIER -> capabilityCommand("airPurifierFanMode", "setAirPurifierFanMode",
                    lookup(PURIFIER_MODE, key, "mode"));
            case VENTILATION_FAN -> capabilityCommand("fanSpeed", "setFanSpeed",
                    lookupInt(FAN_SPEED, key));
        };
    }

    private SmartThingsCommandRequest.Command coolingSetpointCommand(Object temperature) {
        return capabilityCommand("thermostatCoolingSetpoint", "setCoolingSetpoint", toNumber(temperature));
    }

    private SmartThingsCommandRequest.Command capabilityCommand(String capability, String command, Object argument) {
        return new SmartThingsCommandRequest.Command(COMPONENT_MAIN, capability, command, List.of(argument));
    }

    private String lookup(Map<String, String> table, String key, String field) {
        String v = table.get(key);
        if (v == null) {
            throw new SmartThingsApiException("지원하지 않는 " + field + " 값입니다: " + key);
        }
        return v;
    }

    private Integer lookupInt(Map<String, Integer> table, String key) {
        Integer v = table.get(key);
        if (v == null) {
            throw new SmartThingsApiException("지원하지 않는 mode 값입니다: " + key);
        }
        return v;
    }

    private void requireAircon(ActuatorType actuatorType, String field) {
        if (actuatorType != ActuatorType.AIRCON) {
            throw new SmartThingsApiException(
                    "SmartThings 어댑터는 " + field + "를 AIRCON에서만 지원합니다 (actuatorType=" + actuatorType + ")");
        }
    }

    // "24" / 24 / 24.0 -> 정수면 int, 아니면 double. 삼항으로 합치면 int가 double로 승격되므로 return 분리.
    private Object toNumber(Object value) {
        try {
            double d = Double.parseDouble(String.valueOf(value));
            if (d == Math.rint(d) && !Double.isInfinite(d)) {
                return (int) d;
            }
            return d;
        } catch (NumberFormatException e) {
            throw new SmartThingsApiException("temperature는 숫자여야 합니다: " + value);
        }
    }
}
