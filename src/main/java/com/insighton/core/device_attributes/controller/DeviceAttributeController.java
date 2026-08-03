package com.insighton.core.device_attributes.controller;

import com.insighton.core.device_attributes.dto.ActuatorUpdateRequest;
import com.insighton.core.device_attributes.dto.DeviceAttributeResponse;
import com.insighton.core.device_attributes.dto.MetricDefinitionResponse;
import com.insighton.core.device_attributes.entity.MetricDefinition;
import com.insighton.core.device_attributes.service.DeviceAttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sensor/{deviceId}/attribute")
@RequiredArgsConstructor
public class DeviceAttributeController {

    private final DeviceAttributeService attributeService;

    // 기기 속성 전체 목록 조회
    @GetMapping
    public ResponseEntity<List<DeviceAttributeResponse>> getDeviceAttribute(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("deviceId")Long deviceId){
        List<DeviceAttributeResponse> attributeDto = attributeService.getAllAttributeByDeviceId(userId, deviceId);
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
    @PutMapping("/{metricKey}")
    public ResponseEntity<Void> updateActuatorValue(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("deviceId") Long deviceId,
            @PathVariable("metricKey") String metricKey,
            @RequestBody @Valid ActuatorUpdateRequest request){

        attributeService.updateActuatorValue(userId, deviceId, metricKey, request.value());
        return ResponseEntity.noContent().build();
    }
}
