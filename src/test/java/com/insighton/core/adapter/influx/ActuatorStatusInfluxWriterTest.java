package com.insighton.core.adapter.influx;

import com.influxdb.client.WriteApi;
import com.influxdb.client.write.Point;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActuatorStatusInfluxWriterTest {

    @Mock
    private WriteApi writeApi;

    @InjectMocks
    private ActuatorStatusInfluxWriter actuatorStatusInfluxWriter;

    @Test
    @DisplayName("제어기 상태 전환 포인트 기록 성공 (ON -> 1)")
    void writeTransition_on_success() {
        // given
        OffsetDateTime now = OffsetDateTime.now();

        // when
        actuatorStatusInfluxWriter.writeTransition("group-1", "loc-1", "act-1", "SWITCH", "ON", now);

        // then
        verify(writeApi).writePoint(any(Point.class));
    }

    @Test
    @DisplayName("제어기 상태 전환 포인트 기록 성공 (OFF -> 0)")
    void writeTransition_off_success() {
        // given
        OffsetDateTime now = OffsetDateTime.now();

        // when
        actuatorStatusInfluxWriter.writeTransition("group-1", "loc-1", "act-1", "SWITCH", "OFF", now);

        // then
        verify(writeApi).writePoint(any(Point.class));
    }

    @Test
    @DisplayName("제어기 하트비트 포인트 기록 성공")
    void writeHeartbeat_success() {
        // given
        Instant hourMark = Instant.now();

        // when
        actuatorStatusInfluxWriter.writeHeartbeat("group-1", "loc-1", "act-1", "SWITCH", "1", hourMark);

        // then
        verify(writeApi).writePoint(any(Point.class));
    }

    @Test
    @DisplayName("InfluxDB 쓰기 실패 시 예외 격리")
    void write_exception_handled() {
        // given
        doThrow(new RuntimeException("Influx error")).when(writeApi).writePoint(any(Point.class));

        // when
        actuatorStatusInfluxWriter.writeTransition("group-1", "loc-1", "act-1", "SWITCH", "ON", OffsetDateTime.now());

        // then
        verify(writeApi).writePoint(any(Point.class));
    }
}
