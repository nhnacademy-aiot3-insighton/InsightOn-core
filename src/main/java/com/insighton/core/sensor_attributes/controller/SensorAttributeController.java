// SensorAttributeController.java (센서별 속성 전용으로 정리)
package com.insighton.core.sensor_attributes.controller;

import com.insighton.core.sensor_attributes.dto.SensorAttributeResponse;
import com.insighton.core.sensor_attributes.service.SensorAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sensor/{sensor-id}/attribute")
@RequiredArgsConstructor
public class SensorAttributeController {

    private final SensorAttributeService attributeService;

    @GetMapping
    public ResponseEntity<List<SensorAttributeResponse>> getSensorAttribute(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("sensor-id") Long sensorId){
        return ResponseEntity.ok(attributeService.getAllAttributeBySensorId(userId, sensorId));
    }

    @DeleteMapping("/{metric-key}")
    public ResponseEntity<Void> deleteSensorAttribute(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("sensor-id") Long sensorId,
            @PathVariable("metric-key") String metricKey){
        attributeService.deleteAttribute(userId, sensorId, metricKey);
        return ResponseEntity.noContent().build();
    }
}