package com.insighton.core.gateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * LoRaWAN/기타 프로토콜 게이트웨이 인프라 자산 대장. MQTT 접속 정보의 실제 소유 단위
 * TEMP — Group 브랜치 머지 후 FK 제약 추가 필요
 */
@Entity
@Table(name = "gateways")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gateway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gatewayId;

    @Column(name = "groups_id", nullable = false)
    private Long groupsId;

    @Column(name = "gateway_uid", nullable = false, unique = true)
    private String gatewayUid;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol_type", nullable = false)
    private ProtocolType protocolType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "connection_config", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> connectionConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GatewayStatus status;

    @Column(name = "last_heartbeat_at")
    private OffsetDateTime lastHeartbeatAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Gateway(Long groupsId, String gatewayUid, String name, ProtocolType protocolType,
            Map<String, Object> connectionConfig) {
        this.groupsId = groupsId;
        this.gatewayUid = gatewayUid;
        this.name = name;
        this.protocolType = protocolType;
        this.connectionConfig = connectionConfig;
        this.status = GatewayStatus.NORMAL;
    }

    public void update(String name, ProtocolType protocolType, Map<String, Object> connectionConfig) {
        this.name = name;
        this.protocolType = protocolType;
        this.connectionConfig = connectionConfig;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
