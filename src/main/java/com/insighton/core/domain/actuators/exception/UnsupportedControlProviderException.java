package com.insighton.core.domain.actuators.exception;

import com.insighton.core.domain.actuators.control.ControlProvider;

// 구현체가 없는 ControlProvider를 조회했을 때 - 다른 커스텀 예외들처럼 GlobalExceptionHandler에 매핑 필요
// (ActuatorInternalController의 ActuatorType.valueOf() 처리 주석 참고 - 안 걸어두면 500으로 나감)
public class UnsupportedControlProviderException extends RuntimeException {
    public UnsupportedControlProviderException(ControlProvider provider) {
        super("지원하지 않는 제어 공급자입니다: " + provider);
    }
}
