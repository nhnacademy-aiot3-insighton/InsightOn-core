package com.insighton.core.domain.gateway.exception;

public class GatewayNotFoundException extends RuntimeException {

    public GatewayNotFoundException(Long gatewayId) {
        super("게이트웨이를 찾을 수 없습니다. gatewayId=" + gatewayId);
    }

    public GatewayNotFoundException(String message) {
        super(message);
    }

    public static GatewayNotFoundException byGroupId(Long groupId) {
        return new GatewayNotFoundException("게이트웨이를 찾을 수 없습니다. groupId=" + groupId);
    }
}
