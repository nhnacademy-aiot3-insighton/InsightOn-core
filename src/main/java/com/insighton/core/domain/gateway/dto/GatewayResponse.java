package com.insighton.core.domain.gateway.dto;

import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.entity.GatewayStatus;
import com.insighton.core.domain.gateway.entity.ProtocolType;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * connectionConfig은 브로커 인증정보(username/password 등)를 담고 있어 기본적으로는 노출하지 않음.
 * MANAGER 이상 권한으로 자기 그룹의 게이트웨이를 조회할 때만 {@link #from(Gateway, boolean)}의
 * includeConnectionConfig=true로 채워지며, 그 외(MEMBER, 관리자 전체 목록 등)에는 null임.
 */
public record GatewayResponse(
        Long id,
        Long groupsId,
        String name,
        ProtocolType protocolType,
        GatewayStatus status,
        OffsetDateTime lastHeartbeatAt,
        OffsetDateTime createdAt,
        Map<String, Object> connectionConfig
) {
    public static GatewayResponse from(Gateway gateway) {
        return from(gateway, false);
    }

    public static GatewayResponse from(Gateway gateway, boolean includeConnectionConfig) {
        return new GatewayResponse(
                gateway.getGatewayId(),
                gateway.getGroupId(),
                gateway.getName(),
                gateway.getProtocolType(),
                gateway.getStatus(),
                gateway.getLastHeartbeatAt(),
                gateway.getCreatedAt(),
                includeConnectionConfig ? gateway.getConnectionConfig() : null
        );
    }
}
