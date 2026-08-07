package com.insighton.core.sensor_attributes.controller;

import com.insighton.core.sensor_attributes.dto.SensorAttributeResponse;
import com.insighton.core.sensor_attributes.dto.MetricDefinitionResponse;
import com.insighton.core.sensor_attributes.service.SensorAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sensor/attribute")
@RequiredArgsConstructor
public class SensorAttributeController {

    private final SensorAttributeService attributeService;

    // 기기 속성 전체 목록 조회
    @GetMapping("/{sensor-id}")
    public ResponseEntity<List<SensorAttributeResponse>> getSensorAttribute(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("sensor-id")Long sensorId){
        List<SensorAttributeResponse> attributeDto = attributeService.getAllAttributeBySensorId(userId, sensorId);
        return ResponseEntity.ok(attributeDto);
    }


    // 모든 매트릭 조회
    @GetMapping("/definitions")
    public ResponseEntity<List<MetricDefinitionResponse>> getAllMetricDefinitions(){
        return ResponseEntity.ok(attributeService.getAllMetricDefinitions());
    }

}
