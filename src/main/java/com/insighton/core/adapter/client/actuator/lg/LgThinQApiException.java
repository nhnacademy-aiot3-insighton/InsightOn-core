package com.insighton.core.adapter.client.actuator.lg;

import com.insighton.core.domain.actuators.exception.ActuatorControlException;

// LG ThinQ 호환 API 호출 실패 또는 비정상 응답 - 공통 ActuatorControlException으로 취급되어 502로 매핑된다.
public class LgThinQApiException extends ActuatorControlException {

    public LgThinQApiException(String message) {
        super(message);
    }

    public LgThinQApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
