package com.insighton.core.sensors.controller;


import com.insighton.core.sensors.dto.DeviceLocationUpdateRequest;
import com.insighton.core.sensors.dto.DeviceNameUpdateRequest;
import com.insighton.core.sensors.dto.DeviceRequest;
import com.insighton.core.sensors.dto.DeviceResponse;
import com.insighton.core.sensors.service.DeviceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService; // 디바이스 서비스 주입

    // [개선] URL을 /actuators로 바꾸고 메서드명도 createActuator로 변경하여 목적을 명확히 함
    @PostMapping("/actuators") // POST /api/v1/devices/actuators
    public ResponseEntity<Long> createActuator(@RequestBody @Valid DeviceRequest requestDto){
        // 액추에이터 생성을 서비스 계층에 요청
        Long deviceId = deviceService.createActuator(requestDto);
        // 생성된 기기 PK ID 응답 반환
        return ResponseEntity.ok(deviceId);
    }

    // 단일 디바이스 조회 API (GET /api/v1/devices/{id})
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDevice(@PathVariable("id") Long deviceId){
        // 기기 ID로 조회 후 반환
        DeviceResponse responseDto = deviceService.getDeviceById(deviceId);
        return ResponseEntity.ok(responseDto);
    }

    // 통합 검색 API (GET /api/v1/devices/search)
    @GetMapping("/search")
    public ResponseEntity<List<DeviceResponse>> search(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String eui,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long gatewayId,
            @RequestParam(required = false) String deviceName) {

        // 조건 검색 수행 후 리스트 반환
        List<DeviceResponse> result = deviceService.searchDevices(id, eui, locationId, gatewayId, deviceName);
        return ResponseEntity.ok(result);
    }

    // 장치 위치 이동 API (PUT /api/v1/devices/{id}/location)
    @PutMapping("/{id}/location")
    public ResponseEntity<Void> updateDeviceLocation(
            @PathVariable Long id,
            @RequestBody @Valid DeviceLocationUpdateRequest request){
        // 디바이스 위치 업데이트 수행
        deviceService.updateDeviceLocation(id, request.locationId());
        return ResponseEntity.noContent().build();
    }

    // 장치 이름 수정 API (PUT /api/v1/devices/{id}/name)
    @PutMapping("/{id}/name")
    public ResponseEntity<Void> updateDeviceName(
            @PathVariable Long id,
            @RequestBody @Valid DeviceNameUpdateRequest request){
        // 디바이스 이름 업데이트 수행
        deviceService.updateDeviceName(id, request.deviceName());
        return ResponseEntity.noContent().build();
    }

    // 개별 장치 삭제 API (DELETE /api/v1/devices/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id){
        // 디바이스 단일 삭제 수행
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    // 전체 장치 삭제 API (DELETE /api/v1/devices)
    @DeleteMapping
    public ResponseEntity<Void> deleteAllDevice(){
        // 전체 디바이스 삭제 수행
        deviceService.deleteAll();
        return ResponseEntity.noContent().build();
    }
}