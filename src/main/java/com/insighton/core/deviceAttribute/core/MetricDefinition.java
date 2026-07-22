package com.insighton.core.deviceAttribute.core;

import com.insighton.core.error.NoMetricKey;
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
    AC_MODE("ac_mode", "에어컨모드", null);

    private final String metricKey;
    private final String metricName;
    private final String unit;

    public static MetricDefinition fromKey(String metricKey){
        return Arrays.stream(values())
                .filter(m -> m.getMetricKey().equalsIgnoreCase(metricKey))
                .findFirst()
                .orElseThrow(() -> new NoMetricKey(metricKey));
    }
}