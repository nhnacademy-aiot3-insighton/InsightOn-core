package com.insighton.core.sensor_attributes.controller;

import com.insighton.core.sensor_attributes.dto.ActuatorUpdateRequest;
import com.insighton.core.sensor_attributes.dto.SensorAttributeResponse;
import com.insighton.core.sensor_attributes.dto.MetricDefinitionResponse;
import com.insighton.core.sensor_attributes.entity.MetricDefinition;
import com.insighton.core.sensor_attributes.service.SensorAttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sensor/{sensor-id}/attribute")
@RequiredArgsConstructor
public class SensorAttributeController {

    private final SensorAttributeService attributeService;

    // 기기 속성 전체 목록 조회
    @GetMapping
    public ResponseEntity<List<SensorAttributeResponse>> getSensorAttribute(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("sensor-id")Long sensorId){
        List<SensorAttributeResponse> attributeDto = attributeService.getAllAttributeBySensorId(userId, sensorId);
        return ResponseEntity.ok(attributeDto);
    }


    /**
     * 시스템에서 제공하는 모든 메트릭 정의 목록 조회 API
     * 대시보드 위젯 생성 화면등에서 드롭다운 옵션 서빙용
     */
    @GetMapping("/definitions")
    public ResponseEntity<List<MetricDefinitionResponse>> getMetricDefinitions(){
        List<MetricDefinitionResponse> definitionResponses = Arrays.stream(MetricDefinition.values())
                .map(MetricDefinitionResponse::from)
                .toList();
        return ResponseEntity.ok(definitionResponses);
    }

    // 단일 엑충이터 속성 값 변경 제어 API
    @PutMapping("/{metric-key}")
    public ResponseEntity<Void> updateActuatorValue(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("sensor-id") Long sensorId,
            @PathVariable("metric-key") String metricKey,
            @RequestBody @Valid ActuatorUpdateRequest request){

        attributeService.updateActuatorValue(userId, sensorId, metricKey, request.value());
        return ResponseEntity.noContent().build();
    }
}
