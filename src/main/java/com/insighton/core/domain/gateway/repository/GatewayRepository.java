package com.insighton.core.domain.gateway.repository;

import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.entity.ProtocolType;
import java.util.List;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GatewayRepository extends JpaRepository<Gateway, Long> {

    // MqttGatewayWarmUpRunner에서 Mqtt 프로토콜 게이트웨이 서치용
    List<Gateway> findAllByProtocolType(ProtocolType protocolType);
    Optional<Gateway> findByGatewayId(Long gatewayId);
    Optional<Gateway> findByGroupId(Long groupId);
    void deleteByGroupId(Long groupId);
}