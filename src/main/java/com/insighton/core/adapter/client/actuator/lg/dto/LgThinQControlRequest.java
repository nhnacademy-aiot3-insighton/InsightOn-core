package com.insighton.core.adapter.client.actuator.lg.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// LG ThinQ Connect "device control" 요청 형식 (profile subset).
// AIRCON:        { "operation": { "airConOperationMode": "POWER_ON" },
//                  "airConJobMode": { "currentJobMode": "COOL" },
//                  "temperature": { "targetTemperature": 24 } }
// AIR_PURIFIER:  { "operation": { "airConOperationMode": "POWER_ON" },
//                  "airPurifierJobMode": { "currentJobMode": "AUTO" } }
// VENTILATION:   { "operation": { "airConOperationMode": "POWER_ON" },
//                  "windStrength": { "windStrength": "LOW" } }
// 변경하지 않는 property 그룹은 생략한다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LgThinQControlRequest(
        Operation operation,
        AirConJobMode airConJobMode,
        Temperature temperature,
        AirPurifierJobMode airPurifierJobMode,
        WindStrength windStrength
) {

    public record Operation(String airConOperationMode) {
    }

    public record AirConJobMode(String currentJobMode) {
    }

    public record Temperature(Integer targetTemperature) {
    }

    public record AirPurifierJobMode(String currentJobMode) {
    }

    public record WindStrength(String windStrength) {
    }
}
