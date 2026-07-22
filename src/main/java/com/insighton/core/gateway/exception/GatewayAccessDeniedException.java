package com.insighton.core.gateway.exception;

public class GatewayAccessDeniedException extends RuntimeException {

    /**
     * 게이트웨이 접근 거부를 나타내는 예외를 생성합니다.
     *
     * @param message 예외 메시지
     */
    public GatewayAccessDeniedException(String message) {
        super(message);
    }
}
