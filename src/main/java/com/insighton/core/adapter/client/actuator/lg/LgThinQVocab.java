package com.insighton.core.adapter.client.actuator.lg;

import com.insighton.core.domain.actuators.control.NeutralCommand;
import com.insighton.core.domain.actuators.entity.ActuatorType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 중립 명령 (종류, NeutralCommand, 값) → LG ThinQ Connect resource/property의 매핑표.
 *
 * <p><b>실제 endpoint:</b> {@code POST https://api-kic.lgthinq.com/devices/{deviceId}/control}
 * (KR 리전, US=api-aic / EU=api-eic. 로컬은 base-url이 시뮬레이터).
 * body는 이 enum이 만드는 resource 중첩 property bag:
 * {@code { "<resource>": { "<property>": <value> }, ... }} — 변경 안 하는 resource는 생략.
 * 응답 {@code {"messageId":"<uuid>","timestamp":"...","response":{}}} / 오류 {@code {"error":{"code","message"}}}.
 *
 * <p>TEMPERATURE는 값이 동적이라 enum 항목이 아니라 {@link LgThinQControlAssembler}에서 직접 처리
 * (resource {@code "temperature"}, property {@code "targetTemperature"} + {@code "unit":"C"}).
 * [근사] 주석 = 실제 장치 profile 확인 전까지의 근사값 (docs/provider-contract.md §5).
 */
public enum LgThinQVocab {

    // power — 종류별 operation property가 다름
    AC_POWER_ON(ActuatorType.AIRCON, NeutralCommand.POWER, "ON", "operation", "airConOperationMode", "POWER_ON"),
    AC_POWER_OFF(ActuatorType.AIRCON, NeutralCommand.POWER, "OFF", "operation", "airConOperationMode", "POWER_OFF"),
    AP_POWER_ON(ActuatorType.AIR_PURIFIER, NeutralCommand.POWER, "ON", "operation", "airPurifierOperationMode", "POWER_ON"), // [근사]
    AP_POWER_OFF(ActuatorType.AIR_PURIFIER, NeutralCommand.POWER, "OFF", "operation", "airPurifierOperationMode", "POWER_OFF"),
    VF_POWER_ON(ActuatorType.VENTILATION_FAN, NeutralCommand.POWER, "ON", "operation", "airFanOperationMode", "POWER_ON"),   // [근사]
    VF_POWER_OFF(ActuatorType.VENTILATION_FAN, NeutralCommand.POWER, "OFF", "operation", "airFanOperationMode", "POWER_OFF"),

    // 에어컨 mode → airConJobMode.currentJobMode
    AC_COOL(ActuatorType.AIRCON, NeutralCommand.MODE, "COOL", "airConJobMode", "currentJobMode", "COOL"),
    AC_DRY(ActuatorType.AIRCON, NeutralCommand.MODE, "DRY", "airConJobMode", "currentJobMode", "AIR_DRY"),
    AC_FAN(ActuatorType.AIRCON, NeutralCommand.MODE, "FAN", "airConJobMode", "currentJobMode", "FAN"),
    AC_AUTO(ActuatorType.AIRCON, NeutralCommand.MODE, "AUTO", "airConJobMode", "currentJobMode", "AUTO"),
    AC_AIRCLEAN(ActuatorType.AIRCON, NeutralCommand.MODE, "AIRCLEAN", "airConJobMode", "currentJobMode", "AIR_CLEAN"), // LG 에어컨 전용 (공기청정)

    // 에어컨 바람 방향 → windDirection.rotateUpDown (LG 는 상하 스윙 boolean)
    AC_WIND_FIXED(ActuatorType.AIRCON, NeutralCommand.WIND_DIRECTION, "FIXED", "windDirection", "rotateUpDown", Boolean.FALSE),
    AC_WIND_SWING(ActuatorType.AIRCON, NeutralCommand.WIND_DIRECTION, "SWING", "windDirection", "rotateUpDown", Boolean.TRUE),

    // 공기청정기 mode → airPurifierJobMode.currentJobMode
    AP_AUTO(ActuatorType.AIR_PURIFIER, NeutralCommand.MODE, "AUTO", "airPurifierJobMode", "currentJobMode", "AUTO"),
    AP_SLEEP(ActuatorType.AIR_PURIFIER, NeutralCommand.MODE, "SLEEP", "airPurifierJobMode", "currentJobMode", "SLEEP"),
    AP_TURBO(ActuatorType.AIR_PURIFIER, NeutralCommand.MODE, "TURBO", "airPurifierJobMode", "currentJobMode", "CLEAN"), // [근사]

    // 환풍기 mode → airFlow.windStrength
    VF_LOW(ActuatorType.VENTILATION_FAN, NeutralCommand.MODE, "LOW", "airFlow", "windStrength", "LOW"),
    VF_MID(ActuatorType.VENTILATION_FAN, NeutralCommand.MODE, "MID", "airFlow", "windStrength", "MID"),
    VF_HIGH(ActuatorType.VENTILATION_FAN, NeutralCommand.MODE, "HIGH", "airFlow", "windStrength", "HIGH");

    private final ActuatorType type;
    private final NeutralCommand command;
    private final String neutralValue;
    private final String resource;
    private final String property;
    private final Object lgValue;   // String enum 또는 Boolean

    LgThinQVocab(ActuatorType type, NeutralCommand command, String neutralValue,
                 String resource, String property, Object lgValue) {
        this.type = type;
        this.command = command;
        this.neutralValue = neutralValue;
        this.resource = resource;
        this.property = property;
        this.lgValue = lgValue;
    }

    public String resource() {
        return resource;
    }

    public String property() {
        return property;
    }

    public Object lgValue() {
        return lgValue;
    }

    // (종류, 명령, 중립값) 으로 매핑 항목 조회
    public static Optional<LgThinQVocab> find(ActuatorType type, NeutralCommand command, String neutralValue) {
        String needle = neutralValue == null ? null : neutralValue.toUpperCase();
        for (LgThinQVocab v : values()) {
            if (v.type == type && v.command == command && v.neutralValue.equals(needle)) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    // stateKey(mode / windDirection) → 이 종류가 LG ThinQ에서 지원하는 중립값 목록 (Front 조작 UI 렌더 기준)
    public static Map<String, List<String>> supportedValues(ActuatorType type) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (LgThinQVocab v : values()) {
            if (v.type == type && (v.command == NeutralCommand.MODE || v.command == NeutralCommand.WIND_DIRECTION)) {
                out.computeIfAbsent(v.command.stateKey(), k -> new ArrayList<>()).add(v.neutralValue);
            }
        }
        return out;
    }
}
