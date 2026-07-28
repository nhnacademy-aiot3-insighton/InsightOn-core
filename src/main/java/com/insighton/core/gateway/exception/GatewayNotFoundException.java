package com.insighton.core.gateway.exception;

public class GatewayNotFoundException extends RuntimeException {

    public GatewayNotFoundException(Long gatewayId) {
        super("게이트웨이를 찾을 수 없습니다. gatewayId=" + gatewayId);
    }
}
