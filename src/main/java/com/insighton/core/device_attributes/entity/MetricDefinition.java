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

    // 문자열 metricKey를 전달받아 대소문자 구문 없이 해당하는 MetricDefinition Enum 객체를 찾아 반환하는 정적 메서드
    public static MetricDefinition fromKey(String metricKey){
        return Arrays.stream(values()) // 모든 Enum 상수를 스트림으로 변환
                .filter(m -> m.getMetricKey().equalsIgnoreCase(metricKey)) // 대소문자 무시 비교 필터링
                .findFirst() // 조건에 일치하는 첫 번째 항목 탐색
                .orElseThrow(() -> new CustomException(ErrorCode.METRIC_KEY_NOT_FOUND)); // 없을 경우 404 예외 발생
    }
}