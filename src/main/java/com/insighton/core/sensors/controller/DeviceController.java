package com.insighton.core.sensors.controller;

import com.insighton.core.sensors.dto.DeviceLocationUpdateRequest;
import com.insighton.core.sensors.dto.DeviceNameUpdateRequest;
import com.insighton.core.sensors.dto.DeviceResponse;
import com.insighton.core.sensors.dto.DeviceUpdateRequest;
import com.insighton.core.sensors.service.DeviceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sensor")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService; // 디바이스 서비스 주입


    // 단일 디바이스 조회 API (GET /api/v1/devices/{id})
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDevice(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("id") Long deviceId){
        // 기기 ID로 조회 후 반환
        DeviceResponse responseDto = deviceService.getDeviceById(userId, deviceId);
        return ResponseEntity.ok(responseDto);
    }

    // 통합 검색 API (GET /api/v1/devices/search)
    @GetMapping("/search")
    public ResponseEntity<List<DeviceResponse>> search(
            @RequestHeader("X-USER-ID") Long userid,
            @RequestParam Long groupId,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String eui,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String deviceName) {

        // 조건 검색 수행 후 리스트 반환
        List<DeviceResponse> result = deviceService.searchDevices(userid, groupId, id, eui, locationId, deviceName);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateDevice(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long id,
            @RequestBody @Valid DeviceUpdateRequest request
            ){
        deviceService.updateDevice(userId, id, request.locationId(), request.deviceName());
        return ResponseEntity.noContent().build();
    }

    // 개별 장치 삭제 API (DELETE /api/v1/devices/{id}/delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long id){
        // 디바이스 단일 삭제 수행
        deviceService.deleteDevice(userId, id);
        return ResponseEntity.noContent().build();
    }

    // 전체 장치 삭제 API (DELETE /api/v1/devices/deleteAll    ) - groupId 소속만 삭제
    @DeleteMapping
    public ResponseEntity<Void> deleteAllDevice(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam Long groupId){
        // 전체 디바이스 삭제 수행
        deviceService.deleteAll(userId, groupId);
        return ResponseEntity.noContent().build();
    }
}