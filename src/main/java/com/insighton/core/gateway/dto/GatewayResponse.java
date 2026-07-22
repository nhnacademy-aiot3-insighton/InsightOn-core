package com.insighton.core.gateway.dto;

import com.insighton.core.gateway.entity.Gateway;
import com.insighton.core.gateway.entity.GatewayStatus;
import com.insighton.core.gateway.entity.ProtocolType;
import java.time.OffsetDateTime;

/**
 * connection_config은 의도적으로 노출하지 않음 — 브로커 인증정보(username/password 등)를 담고 있어
 * 목록/단건 조회 응답에 그대로 내보내면 안 됨. 필요해지면 별도 상세 조회 경로에서 권한 체크 후 노출 검토.
 */
public record GatewayResponse(
        Long id,
        Long groupsId,
        String gatewayUid,
        String name,
        ProtocolType protocolType,
        GatewayStatus status,
        OffsetDateTime lastHeartbeatAt,
        OffsetDateTime createdAt
) {
    /**
     * 게이트웨이 엔터티의 주요 정보를 응답 DTO로 변환한다.
     *
     * @param gateway 변환할 게이트웨이 엔터티
     * @return 게이트웨이 정보가 매핑된 응답 DTO
     */
    public static GatewayResponse from(Gateway gateway) {
        return new GatewayResponse(
                gateway.getGatewayId(),
                gateway.getGroupsId(),
                gateway.getGatewayUid(),
                gateway.getName(),
                gateway.getProtocolType(),
                gateway.getStatus(),
                gateway.getLastHeartbeatAt(),
                gateway.getCreatedAt()
        );
    }
}
