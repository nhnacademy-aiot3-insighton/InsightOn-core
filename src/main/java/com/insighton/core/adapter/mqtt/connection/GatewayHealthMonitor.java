package com.insighton.core.adapter.mqtt.connection;

import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.entity.GatewayStatus;
import com.insighton.core.domain.gateway.repository.GatewayRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayHealthMonitor {

    private final DynamicMqttGatewayManager gatewayManager;
    private final GatewayHeartbeatTracker heartbeatTracker;
    private final GatewayRepository gatewayRepository;

    @Value("${gateway.health.fault-threshold-seconds:1800}")
    private long faultThresholdSeconds;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void checkGatewayHealth() {

        Set<Long> ownedGatewayIds = gatewayManager.getRegisterGatewayIds();

        if(ownedGatewayIds.isEmpty()) {
            return;
        }

        Map<Long, Instant> heartbeats = heartbeatTracker.snapshot();
        Instant now = Instant.now();

        List<Gateway> gateways = gatewayRepository.findAllById(ownedGatewayIds);

        for(Gateway gateway : gateways) {
            Instant lastSeen = heartbeats.get(gateway.getGatewayId());

            if(lastSeen == null) {
                //아직 패킷을 못받은 gateway 판단 근거 없음
                continue;
            }

            long idleSeconds = Duration.between(lastSeen, now).getSeconds();

            if(idleSeconds >= faultThresholdSeconds) {
                if(gateway.getStatus() != GatewayStatus.FAULT) {
                    gateway.markFault();
                    log.warn("Gateway {} 마지막 수신 {}초 전 - FAULT 전환", gateway.getGatewayId(), idleSeconds);
                }
                continue;
            }

            OffsetDateTime seenAt = OffsetDateTime.ofInstant(lastSeen, ZoneOffset.UTC);

            if(gateway.getLastHeartbeatAt() == null || seenAt.isAfter(gateway.getLastHeartbeatAt())) {
                boolean wasFault = gateway.getStatus() == GatewayStatus.FAULT;
                gateway.recordHeartbeat(seenAt);

                if(wasFault) {
                    log.info("Gateway {} 하트비트 재개 감지 — ACTIVE로 복구", gateway.getGatewayId());
                }
            }
        }
    }

}
