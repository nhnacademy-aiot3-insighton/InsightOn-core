package com.insighton.core.deviceAttribute.controller;

import com.insighton.core.deviceAttribute.dto.DeviceAttributeDto;
import com.insighton.core.deviceAttribute.service.DeviceAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/devices/{deviceId}/attribute")
@RequiredArgsConstructor
public class DeviceAttributeController {

    private final DeviceAttributeService attributeService;


    // 기기 속성 목록 조회
    @GetMapping
    public ResponseEntity<List<DeviceAttributeDto>> getDeviceAttribute(@PathVariable("deviceId")Long deviceId){
        List<DeviceAttributeDto> attributeDto = attributeService.getAllAttributeByDeviceId(deviceId);
        return ResponseEntity.ok(attributeDto);
    }
}
