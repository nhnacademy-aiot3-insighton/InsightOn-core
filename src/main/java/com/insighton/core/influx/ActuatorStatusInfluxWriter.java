package com.insighton.core.influx;

import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.insighton.core.influx.dto.ActuatorStatusMeasurement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActuatorStatusInfluxWriter {

    private final WriteApi writeApi;

    // POWER_STATUS 명령 실행 시점(전환 이벤트) 기록
    @Async("telemetryDispatchExecutor")
    public void writeTransition(String groupId, String locationId, String actuatorId, String actuatorType,
                                String commandValue, OffsetDateTime executedAt) {
        write(new ActuatorStatusMeasurement(executedAt.toInstant(), groupId, locationId, actuatorId, actuatorType,
                toStatusValue(commandValue)));
    }

    // 정각 하트비트 - 전환 여부 무관하게 현재 상태를 그대로 재기록
    @Async("telemetryDispatchExecutor")
    public void writeHeartbeat(String groupId, String locationId, String actuatorId, String actuatorType,
                               String currentPowerValue, Instant hourMark) {
        write(new ActuatorStatusMeasurement(hourMark, groupId, locationId, actuatorId, actuatorType,
                toStatusValue(currentPowerValue)));
    }

    // "ON"/"1"/"true" -> 1, 그 외(OFF, null 등) -> 0
    private int toStatusValue(String value) {
        if (value == null) {
            return 0;
        }
        return ("ON".equalsIgnoreCase(value) || "1".equals(value) || "true".equalsIgnoreCase(value)) ? 1 : 0;
    }

    private void write(ActuatorStatusMeasurement m) {
        try {
            Point point = Point.measurement("actuator_status")
                    .addTag("group_id", m.groupId())
                    .addTag("location_id", m.locationId())
                    .addTag("actuator_id", m.actuatorId())
                    .addTag("actuator_type", m.actuatorType())
                    .addField("status_value", m.statusValue())
                    .time(m.time(), WritePrecision.MS);

            writeApi.writePoint(point);
        } catch (Exception e) {
            log.warn("InfluxDB actuator_status 포인트 큐잉 실패 (actuatorId = {})", m.actuatorId(), e);
        }
    }
}