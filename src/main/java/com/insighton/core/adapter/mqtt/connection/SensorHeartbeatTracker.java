package com.insighton.core.adapter.mqtt.connection;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SensorHeartbeatTracker {

    private final Map<Long, Instant> lastSeenAt = new ConcurrentHashMap<>();

    /**
     * 센서로부터 패킷 수신 했을 때의 시간 기록
     * @param sensorId 패킷을 보낸 센서ID
     */
    public void recordHeartbeat(Long sensorId) {
        lastSeenAt.put(sensorId, Instant.now());
    }

    /**
     * 현재까지 기록된 전체 센서의 마지막 수신 시간 스냅샷 반환
     * @return lastSeenAt Map 복사본
     */
    public Map<Long, Instant> snapshot() {
        return Map.copyOf(lastSeenAt);
    }
}
