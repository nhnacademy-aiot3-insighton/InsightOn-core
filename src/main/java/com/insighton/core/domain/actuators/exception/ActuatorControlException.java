package com.insighton.core.domain.actuators.exception;

// 공급자 Adapter가 외부 제어 API 호출/응답 처리에 실패했을 때 던지는 공통 예외.
// 공급자별 예외(SmartThingsApiException 등)는 이 예외를 상속한다.
// GlobalExceptionHandler에서 502(Bad Gateway)로 매핑 - 외부 공급자 장애이지 CORE 버그가 아님을 표현.
public class ActuatorControlException extends RuntimeException {

    public ActuatorControlException(String message) {
        super(message);
    }

    public ActuatorControlException(String message, Throwable cause) {
        super(message, cause);
    }
}
