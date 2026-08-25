package com.insighton.core.adapter.mqtt.connection;

import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.repository.SensorRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SensorHeartbeatFlusher {

    private final SensorHeartbeatTracker heartbeatTracker;
    private final SensorRepository sensorRepository;

    @Scheduled(fixedDelay = 150_000)
    @Transactional
    public void flushHeartbeats() {
        log.info("SensorHeartbeatFlusher Scheduler 동작");
        Map<Long, Instant> heartbeats = heartbeatTracker.snapshot();

        if(heartbeats.isEmpty()) {
            log.info("heartbeats Snapshot is empty - 스케줄러 종료");
            return;
        }

        List<Sensor> sensors = sensorRepository.findAllById(heartbeats.keySet());

        for(Sensor sensor : sensors) {
            Instant lastSeen = heartbeats.get(sensor.getSensorId());
            OffsetDateTime seenAt = OffsetDateTime.ofInstant(lastSeen, ZoneOffset.UTC);

            if(sensor.getLastSeenAt() == null || seenAt.isAfter(sensor.getLastSeenAt())) {
                sensor.updateLastSeen(seenAt);
            }
        }
        log.info("SensorHeartbeatFlusher Scheduler - 정상동작 완료. sensor 마지막 통신 시간 최신화 완료");
    }
}
