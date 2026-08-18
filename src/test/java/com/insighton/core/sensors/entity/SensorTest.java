package com.insighton.core.sensors.entity;

import com.insighton.core.domain.sensors.entity.Sensor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensorTest {

    @Test
    @DisplayName("updateLastSeen - 마지막 통신 시각을 현재로 갱신")
    void 통신시각_갱신() {
        Sensor sensor = Sensor.builder().sensorId(1L).build();

        sensor.updateLastSeen();

        assertThat(sensor.getLastSeenAt()).isNotNull();
    }
}
