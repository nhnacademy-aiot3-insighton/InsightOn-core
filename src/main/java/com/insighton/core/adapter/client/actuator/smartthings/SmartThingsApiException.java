package com.insighton.core.adapter.client.actuator.smartthings;

import com.insighton.core.domain.actuators.exception.ActuatorControlException;

// SmartThings 호환 API 호출 실패 또는 비정상 응답 - 공통 ActuatorControlException으로 취급되어 502로 매핑된다.
public class SmartThingsApiException extends ActuatorControlException {

    public SmartThingsApiException(String message) {
        super(message);
    }

    public SmartThingsApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
