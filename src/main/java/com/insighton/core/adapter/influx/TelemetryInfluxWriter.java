package com.insighton.core.adapter.influx;

import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.insighton.core.adapter.influx.dto.DynamicTelemetryMeasurement;
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
     * 정제된 텔레메트리 측정값 하나를 InfluxDB에 비동기로 적재함.
     * {@code @Async("telemetryDispatchExecutor")}로 별도 스레드풀에서 처리되어, InfluxDB 응답을
     * 기다리느라 MQTT 리스너 스레드가 막히지 않게 함. groupId/locationId/sensorEui/sensorName은
     * 태그로, 나머지 동적 메트릭(co2, humidity 등)은 {@code addFields()}로 필드에 그대로 펼쳐서 적재함.
     * 쓰기 실패 시 예외를 던지지 않고 로그만 남기고 조용히 드롭함(Fail-Silent) — InfluxDB 장애가
     * 패킷 처리 파이프라인 전체로 전파되지 않게 하기 위함.
     *
     * @param measurement 적재할 텔레메트리 측정값
     */
    @Async("telemetryDispatchExecutor")
    public void write(DynamicTelemetryMeasurement measurement) {
        try {
            Point point = Point.measurement("sensor_data")
                    .addTag("groupId", measurement.groupId())
                    .addTag("location_id", measurement.locationId())
                    .addTag("sensor_eui", measurement.sensorEui())
                    .addTag("sensor_name", measurement.sensorName())
                    .addFields(measurement.fields())
                    .time(measurement.time(), WritePrecision.MS);

            writeApi.writePoint(point);
        } catch (Exception e) {
            log.warn("InfluxDB 포인트 큐잉 실패, 데이터 드롭 (locationId = {}, sensorEui = {})",
                    measurement.locationId(), measurement.sensorEui(), e);
        }
    }
}
