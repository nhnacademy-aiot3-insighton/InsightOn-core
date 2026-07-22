package com.insighton.core.deviceAttribute.controller;

import com.insighton.core.deviceAttribute.dto.DeviceAttributeDto;
import com.insighton.core.deviceAttribute.dto.MetricDefinitionResponse;
import com.insighton.core.deviceAttribute.entity.MetricDefinition;
import com.insighton.core.deviceAttribute.service.DeviceAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("api/devices/{deviceId}/attribute")
@RequiredArgsConstructor
public class DeviceAttributeController {

    private final DeviceAttributeService attributeService;

    // 기기 속성 전체 목록 조회
    @GetMapping
    public ResponseEntity<List<DeviceAttributeDto>> getDeviceAttribute(@PathVariable("deviceId")Long deviceId){
        List<DeviceAttributeDto> attributeDto = attributeService.getAllAttributeByDeviceId(deviceId);
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

}
