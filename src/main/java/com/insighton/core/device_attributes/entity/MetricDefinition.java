package com.insighton.core.device_attributes.entity;


import com.insighton.core.exception.CustomException;
import com.insighton.core.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;


@Getter
@RequiredArgsConstructor
public enum MetricDefinition {
    CO2("co2", "이산화탄소", "ppm"),
    TEMPERATURE("temperature", "온도", "°C"),
    HUMIDITY("humidity", "습도", "%"),
    POWER_STATUS("power_status", "전원상태", null),
    AC_MODE("ac_mode", "에어컨모드", null),
    AIR_PURIFIER_MODE("ap_mode", "공기청정기모드", null),
    VENTILATION_FAN("vf_mode", "환풍기모드", null);

    private final String metricKey;
    private final String metricName;
    private final String unit;

    public static MetricDefinition fromKey(String metricKey){
        return Arrays.stream(values())
                .filter(m -> m.getMetricKey().equalsIgnoreCase(metricKey))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.METRIC_KEY_NOT_FOUND));
    }
}