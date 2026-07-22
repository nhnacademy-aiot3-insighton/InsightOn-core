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

    /**
     * 게이트웨이를 생성하고 초기 상태를 정상으로 설정한다.
     *
     * @param groupsId 게이트웨이가 속한 그룹의 식별자
     * @param gatewayUid 게이트웨이의 고유 식별자
     * @param name 게이트웨이 이름
     * @param protocolType 게이트웨이가 사용하는 프로토콜 유형
     * @param connectionConfig 게이트웨이 연결 설정
     */
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

    /**
     * 게이트웨이의 이름, 프로토콜 유형 및 연결 설정을 수정한다.
     *
     * @param name 게이트웨이 이름
     * @param protocolType 게이트웨이 프로토콜 유형
     * @param connectionConfig 게이트웨이 연결 설정
     */
    public void update(String name, ProtocolType protocolType, Map<String, Object> connectionConfig) {
        this.name = name;
        this.protocolType = protocolType;
        this.connectionConfig = connectionConfig;
    }

    /**
     * 엔티티가 처음 저장되기 직전에 생성 시각을 현재 시각으로 설정합니다.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
