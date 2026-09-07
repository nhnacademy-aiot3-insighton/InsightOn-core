package com.insighton.core.adapter.mqtt.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.repository.SensorRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SensorHeartbeatFlusherTest {

    @Mock
    private SensorHeartbeatTracker heartbeatTracker;
    @Mock
    private SensorRepository sensorRepository;

    private SensorHeartbeatFlusher flusher;

    @BeforeEach
    void setUp() {
        flusher = new SensorHeartbeatFlusher(heartbeatTracker, sensorRepository);
    }

    @Test
    void 스냅샷이_비어있으면_DB_조회_없이_종료한다() {
        given(heartbeatTracker.snapshot()).willReturn(Map.of());

        flusher.flushHeartbeats();

        verify(sensorRepository, never()).findAllById(any());
    }

    @Test
    void 기존_기록보다_최신이면_마지막_수신시각을_갱신한다() {
        Sensor sensor = Sensor.builder().sensorId(1L).build();
        Instant now = Instant.now();
        given(heartbeatTracker.snapshot()).willReturn(Map.of(1L, now));
        given(sensorRepository.findAllById(Set.of(1L))).willReturn(List.of(sensor));

        flusher.flushHeartbeats();

        assertThat(sensor.getLastSeenAt()).isEqualTo(OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
    }

    @Test
    void 기존_기록보다_과거면_갱신하지_않는다() {
        Sensor sensor = Sensor.builder().sensorId(1L).build();
        OffsetDateTime recent = OffsetDateTime.now(ZoneOffset.UTC);
        sensor.updateLastSeen(recent);

        Instant staleHeartbeat = recent.minusMinutes(10).toInstant();
        given(heartbeatTracker.snapshot()).willReturn(Map.of(1L, staleHeartbeat));
        given(sensorRepository.findAllById(Set.of(1L))).willReturn(List.of(sensor));

        flusher.flushHeartbeats();

        assertThat(sensor.getLastSeenAt()).isEqualTo(recent); // 그대로 유지
    }
}
