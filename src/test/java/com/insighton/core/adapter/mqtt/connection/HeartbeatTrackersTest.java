package com.insighton.core.adapter.mqtt.connection;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Gateway/Sensor 하트비트 트래커는 로직이 동일한 얇은 ConcurrentHashMap 래퍼라 한 파일에서 함께 검증.
 */
class HeartbeatTrackersTest {

    @Test
    void GatewayHeartbeatTracker_기록한_시각을_조회할_수_있다() {
        GatewayHeartbeatTracker tracker = new GatewayHeartbeatTracker();

        tracker.recordHeartbeat(1L);

        assertThat(tracker.get(1L)).isPresent();
        assertThat(tracker.get(2L)).isEmpty();
        assertThat(tracker.snapshot()).containsOnlyKeys(1L);
    }

    @Test
    void GatewayHeartbeatTracker_snapshot은_원본과_독립된_복사본이다() {
        GatewayHeartbeatTracker tracker = new GatewayHeartbeatTracker();
        tracker.recordHeartbeat(1L);

        var snapshot = tracker.snapshot();
        tracker.recordHeartbeat(2L);

        assertThat(snapshot).containsOnlyKeys(1L); // 이후 기록이 이전 스냅샷에 반영되지 않음
    }

    @Test
    void SensorHeartbeatTracker_기록한_시각을_스냅샷으로_조회할_수_있다() {
        SensorHeartbeatTracker tracker = new SensorHeartbeatTracker();
        Instant before = Instant.now();

        tracker.recordHeartbeat(10L);

        assertThat(tracker.snapshot()).containsKey(10L);
        assertThat(tracker.snapshot().get(10L)).isAfterOrEqualTo(before);
    }
}
