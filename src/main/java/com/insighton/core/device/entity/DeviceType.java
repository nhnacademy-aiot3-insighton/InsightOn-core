package com.insighton.core.device.entity;

import lombok.Getter;

/**
 * 기기 대분류 스펙 분류 ENUM
 */
@Getter
public enum DeviceType {
    SENSOR,           // 센서
    AIRCON,           // 에어컨
    AIR_PURIFIER,     // 공기청정기
    VENTILATION_FAN   // 환기팬
}