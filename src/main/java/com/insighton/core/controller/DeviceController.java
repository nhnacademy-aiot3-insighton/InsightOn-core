package com.insighton.core.controller;


import com.insighton.core.dto.DeviceResponseDto;
import com.insighton.core.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    @GetMapping("/search")
    public ResponseEntity<List<DeviceResponseDto>> search(
            //@RequestParam(required = false) 선택적으로 입력하도록 허용
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String eui,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long gatewayId,
            @RequestParam(required = false) String name) {

        return ResponseEntity.ok(deviceService.searchDevices(id, eui, locationId, gatewayId, name));
    }
}