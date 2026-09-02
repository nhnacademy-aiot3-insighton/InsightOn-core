package com.insighton.core.domain.actuators.control;

import com.insighton.core.domain.actuators.entity.ActuatorType;

import java.util.UUID;

// 액추에이터를 공급자에 연결할 때 CORE가 부여하는 장치 식별자 생성기.
// 형식: {공급자}-{종류}-{랜덤8자}  예) lg-aircon-a1b2c3d4, st-purifier-9f8e7d6c
// 지금은 독립 시뮬레이터가 상태를 저장하지 않고 아무 deviceId나 받으므로 CORE가 자체 생성한다.
// 실제 SmartThings/LG ThinQ 연동으로 바뀌면 공급자 계정에서 발급한 deviceId(UUID)를 그대로 저장하고
// 이 생성기는 사용하지 않는다.
public final class ExternalDeviceIdGenerator {

    private ExternalDeviceIdGenerator() {
    }

    public static String generate(ControlProvider provider, ActuatorType actuatorType) {
        return providerPrefix(provider) + "-" + typePrefix(actuatorType) + "-"
                + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String providerPrefix(ControlProvider provider) {
        return switch (provider) {
            case SMART_THINGS -> "st";
            case LG_THINQ -> "lg";
        };
    }

    private static String typePrefix(ActuatorType actuatorType) {
        return switch (actuatorType) {
            case AIRCON -> "aircon";
            case AIR_PURIFIER -> "purifier";
            case VENTILATION_FAN -> "fan";
        };
    }
}
