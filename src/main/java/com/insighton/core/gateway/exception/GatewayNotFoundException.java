package com.insighton.core.gateway.exception;

public class GatewayNotFoundException extends RuntimeException {

    /**
     * 게이트웨이를 찾을 수 없음을 나타내는 예외를 생성합니다.
     *
     * @param gatewayId 찾을 수 없는 게이트웨이의 식별자
     */
    public GatewayNotFoundException(Long gatewayId) {
        super("게이트웨이를 찾을 수 없습니다. gatewayId=" + gatewayId);
    }
}
