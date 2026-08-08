package com.insighton.core.adapter.mqtt.connection;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 게이트웨이별 마지막 패킷 수신 시각을 메모리에만 기록하는 트래커.
 * DB(last_heartbeat_at)에는 쓰지 않음 — 패킷마다 DB를 건드리면 핫패스가 막히므로,
 * 실제 영속화/FAULT 판정은 스케줄러가 이 트래커를 주기적으로 읽어서 처리
 */
//TODO: 트래커 읽을 스케줄러 구현 필요
@Component
public class GatewayHeartbeatTracker {

    private final Map<Long, Instant> lastSeenAt = new ConcurrentHashMap<>();

    /**
     * 게이트웨이로부터 패킷을 수신했다는 사실을 현재 시각으로 기록함.
     *
     * @param gatewaysId 패킷을 보낸 게이트웨이 PK
     */
    public void recordHeartbeat(Long gatewaysId) {
        lastSeenAt.put(gatewaysId, Instant.now());
    }

    /**
     * 게이트웨이의 마지막 수신 시각 조회
     *
     * @param gatewaysId 조회할 게이트웨이 PK
     * @return 마지막 수신 시각, 기록이 없으면 빈 Optional
     */
    public Optional<Instant> get(Long gatewaysId) {
        return Optional.ofNullable(lastSeenAt.get(gatewaysId));
    }

    /**
     * 현재까지 기록된 전체 게이트웨이의 마지막 수신 시각 스냅샷 반환
     * 스케줄러가 주기적으로 순회하며 DB 반영/FAULT 판정에 사용
     *
     * @return 게이트웨이 ID를 키로 하는 마지막 수신 시각 맵의 복사본
     */
    public Map<Long, Instant> snapshot() {
        return Map.copyOf(lastSeenAt);
    }
}