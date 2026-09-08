package com.insighton.core.adapter.client.actuator.smartthings;

import com.insighton.core.domain.actuators.control.NeutralCommand;
import com.insighton.core.domain.actuators.entity.ActuatorType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 중립 명령 (종류, NeutralCommand, 값) → SmartThings capability 명령의 매핑표.
 *
 * <p><b>실제 endpoint:</b> {@code POST https://api.smartthings.com/v1/devices/{deviceId}/commands}
 * (로컬은 base-url이 시뮬레이터). body는 이 enum이 만드는 command의 배열:
 * {@code {"commands":[{"component":"main","capability":<capability>,"command":<command>,"arguments":[<argument>]}]}}
 * 응답 {@code {"results":[{"id":"<uuid>","status":"ACCEPTED"}]}}.
 *
 * <p>TEMPERATURE는 값이 동적(정수)이라 enum 항목이 아니라 {@link SmartThingsCommandAssembler}에서 직접 처리.
 * [근사] 주석 = 실제 장치 profile 확인 전까지의 근사값.
 */
public enum SmartThingsVocab {

    // power — 종류 무관 (type == null = wildcard)
    POWER_ON(null, NeutralCommand.POWER, "ON", "switch", "on", null),
    POWER_OFF(null, NeutralCommand.POWER, "OFF", "switch", "off", null),

    // 에어컨 mode → airConditionerMode
    AC_COOL(ActuatorType.AIRCON, NeutralCommand.MODE, "COOL", Keys.AIRCONDITIONER_MODE, Keys.SET_AIRCONDITIONER_MODE, "cool"),
    AC_DRY(ActuatorType.AIRCON, NeutralCommand.MODE, "DRY", Keys.AIRCONDITIONER_MODE, Keys.SET_AIRCONDITIONER_MODE, "dry"),
    AC_FAN(ActuatorType.AIRCON, NeutralCommand.MODE, "FAN", Keys.AIRCONDITIONER_MODE, Keys.SET_AIRCONDITIONER_MODE, "wind"),
    AC_AUTO(ActuatorType.AIRCON, NeutralCommand.MODE, "AUTO", Keys.AIRCONDITIONER_MODE, Keys.SET_AIRCONDITIONER_MODE, "auto"),
    // AIRCLEAN 은 LG 전용 — SmartThings 항목 없음 (find() 실패 시 어댑터가 거절)

    // 에어컨 바람 방향 → fanOscillationMode (SmartThings 는 단일 enum)
    AC_WIND_FIXED(ActuatorType.AIRCON, NeutralCommand.WIND_DIRECTION, "FIXED", "fanOscillationMode", "setFanOscillationMode", "fixed"),
    AC_WIND_SWING(ActuatorType.AIRCON, NeutralCommand.WIND_DIRECTION, "SWING", "fanOscillationMode", "setFanOscillationMode", "all"),

    // 공기청정기 mode → airPurifierFanMode
    AP_AUTO(ActuatorType.AIR_PURIFIER, NeutralCommand.MODE, "AUTO", Keys.AIR_PURIFIER_FAN_MODE, Keys.SET_AIR_PURIFIER_FAN_MODE, "auto"),
    AP_SLEEP(ActuatorType.AIR_PURIFIER, NeutralCommand.MODE, "SLEEP", Keys.AIR_PURIFIER_FAN_MODE, Keys.SET_AIR_PURIFIER_FAN_MODE, "sleep"),
    AP_TURBO(ActuatorType.AIR_PURIFIER, NeutralCommand.MODE, "TURBO", Keys.AIR_PURIFIER_FAN_MODE, Keys.SET_AIR_PURIFIER_FAN_MODE, "high"), // [근사]

    // 환풍기 mode → fanSpeed (정수 인자)
    VF_LOW(ActuatorType.VENTILATION_FAN, NeutralCommand.MODE, "LOW", Keys.FAN_SPEED, Keys.SET_FAN_SPEED, 1),
    VF_MID(ActuatorType.VENTILATION_FAN, NeutralCommand.MODE, "MID", Keys.FAN_SPEED, Keys.SET_FAN_SPEED, 2),
    VF_HIGH(ActuatorType.VENTILATION_FAN, NeutralCommand.MODE, "HIGH", Keys.FAN_SPEED, Keys.SET_FAN_SPEED, 3);

    // enum 상수 인자에서는 같은 enum의 static 필드를 못 참조하므로(forward reference) 별도 홀더에 모음
    private static final class Keys {
        private static final String AIRCONDITIONER_MODE = "airConditionerMode";
        private static final String SET_AIRCONDITIONER_MODE = "setAirConditionerMode";
        private static final String AIR_PURIFIER_FAN_MODE = "airPurifierFanMode";
        private static final String SET_AIR_PURIFIER_FAN_MODE = "setAirPurifierFanMode";
        private static final String FAN_SPEED = "fanSpeed";
        private static final String SET_FAN_SPEED = "setFanSpeed";

        private Keys() {
        }
    }

    private final ActuatorType type;   // null = 종류 무관
    private final NeutralCommand command;
    private final String neutralValue;
    private final String capability;
    private final String stCommand;
    private final Object argument;      // null = arguments:[]

    SmartThingsVocab(ActuatorType type, NeutralCommand command, String neutralValue,
                     String capability, String stCommand, Object argument) {
        this.type = type;
        this.command = command;
        this.neutralValue = neutralValue;
        this.capability = capability;
        this.stCommand = stCommand;
        this.argument = argument;
    }

    public String capability() {
        return capability;
    }

    public String stCommand() {
        return stCommand;
    }

    public List<Object> arguments() {
        return argument == null ? List.of() : List.of(argument);
    }

    // (종류, 명령, 중립값) 으로 매핑 항목 조회
    public static Optional<SmartThingsVocab> find(ActuatorType type, NeutralCommand command, String neutralValue) {
        String needle = neutralValue == null ? null : neutralValue.toUpperCase(Locale.ROOT);
        for (SmartThingsVocab v : values()) {
            boolean typeOk = v.type == null || v.type == type;
            if (typeOk && v.command == command && v.neutralValue.equals(needle)) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    // stateKey(mode / windDirection) → 이 종류가 SmartThings에서 지원하는 중립값 목록 (Front 조작 UI 렌더 기준)
    public static Map<String, List<String>> supportedValues(ActuatorType type) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (SmartThingsVocab v : values()) {
            if (v.type == type && (v.command == NeutralCommand.MODE || v.command == NeutralCommand.WIND_DIRECTION)) {
                out.computeIfAbsent(v.command.stateKey(), k -> new ArrayList<>()).add(v.neutralValue);
            }
        }
        return out;
    }
}
