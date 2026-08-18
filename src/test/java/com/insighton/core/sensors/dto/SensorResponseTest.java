package com.insighton.core.sensors.dto;

import com.insighton.core.domain.sensors.dto.SensorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SensorResponseTest {

    @Test
    @DisplayName("isOnline - 5분 이내 통신했으면 true")
    void 온라인_5분이내() {
        SensorResponse response = new SensorResponse(
                1L, 10L, 20L, "EUI-1", "센서", OffsetDateTime.now(), OffsetDateTime.now().minusMinutes(1));

        assertThat(response.isOnline()).isTrue();
    }

    @Test
    @DisplayName("isOnline - 5분 넘게 통신 없으면 false")
    void 오프라인_5분초과() {
        SensorResponse response = new SensorResponse(
                1L, 10L, 20L, "EUI-1", "센서", OffsetDateTime.now(), OffsetDateTime.now().minusMinutes(10));

        assertThat(response.isOnline()).isFalse();
    }

    @Test
    @DisplayName("isOnline - lastSeenAt이 없으면 false")
    void 오프라인_통신이력없음() {
        SensorResponse response = new SensorResponse(
                1L, 10L, 20L, "EUI-1", "센서", OffsetDateTime.now(), null);

        assertThat(response.isOnline()).isFalse();
    }
}
