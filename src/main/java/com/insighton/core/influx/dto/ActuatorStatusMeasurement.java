package com.insighton.core.influx.dto;


import java.time.Instant;

// actuator_status measurement 포인트 1개를 표현하는 불변 객체
public record ActuatorStatusMeasurement(
        Instant time,
        String groupId,
        String locationId,
        String actuatorId,
        String actuatorType,
        int statusValue // 1=ON, 0=OFF
) {}
