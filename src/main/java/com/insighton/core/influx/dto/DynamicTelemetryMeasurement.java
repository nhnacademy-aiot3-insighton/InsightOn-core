package com.insighton.core.influx.dto;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import java.time.Instant;
import java.util.Map;

/**
 * 어떤 가변 센서 메트릭이 들어와도 동적으로 필드를 생성해 적재하는 불변 객체
 */
public record DynamicTelemetryMeasurement(
    Instant time,
    String groupId,
    String locationId,
    String deviceEui,
    String deviceName,
    Map<String, Object> fields
) {}