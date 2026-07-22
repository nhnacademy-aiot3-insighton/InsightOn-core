package com.insighton.core.influx;

import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.insighton.core.influx.dto.DynamicTelemetryMeasurement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryInfluxWriter {

    private final WriteApi writeApi;

    /**
     * 텔레메트리 측정값을 InfluxDB에 기록하도록 큐에 추가합니다.
     *
     * @param measurement InfluxDB에 기록할 텔레메트리 측정값
     */
    @Async("telemetryDispatchExecutor")
    public void write(DynamicTelemetryMeasurement measurement) {
        try {
            Point point = Point.measurement("sensor_data")
                    .addTag("groupId", measurement.groupId())
                    .addTag("location_id", measurement.locationId())
                    .addTag("device_eui", measurement.deviceEui())
                    .addTag("device_name", measurement.deviceName())
                    .addFields(measurement.fields())
                    .time(measurement.time(), WritePrecision.MS);

            writeApi.writePoint(point);
        } catch (Exception e) {
            log.warn("InfluxDB 포인트 큐잉 실패, 데이터 드롭 (locationId = {}, deviceEui = {})",
                    measurement.locationId(), measurement.deviceEui(), e);
        }
    }
}
