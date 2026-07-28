package com.insighton.core.devices.controller;


import com.insighton.core.devices.dto.DeviceLocationUpdateRequest;
import com.insighton.core.devices.dto.DeviceNameUpdateRequest;
import com.insighton.core.devices.dto.DeviceRequest;
import com.insighton.core.devices.dto.DeviceResponse;
import com.insighton.core.devices.service.DeviceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    // 장치 등록 API
    // DeviceRequestDto를 받아 장치를 생성하고 생성된 ID를 반환
    @PostMapping
    public ResponseEntity<Long> createDevice(@RequestBody @Valid DeviceRequest requestDto){
        Long deviceId = deviceService.createDevice(requestDto);
        return ResponseEntity.ok(deviceId);
    }

    // 단일 deviceId 검색
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDevice(@PathVariable("id") Long deviceId){

        DeviceResponse responseDto = deviceService.getDeviceById(deviceId);
        return ResponseEntity.ok(responseDto);
    }


    // 통합 조건 검색 API
    @GetMapping("/search")
    public ResponseEntity<List<DeviceResponse>> search(
            //@RequestParam(required = false) 선택적으로 입력하도록 허용
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String eui,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long gatewayId,
            @RequestParam(required = false) String deviceName) { // name -> deviceName

        // 파라미터 조건에 따라 적절한 데이터를 조회하여 반환
        List<DeviceResponse> result = deviceService.searchDevices(id, eui, locationId, gatewayId, deviceName);
        return ResponseEntity.ok(result);
    }

    // 장치 위치 이동(수정) API - 전용 DTO 적용
    @PutMapping("/{id}/location")
    public ResponseEntity<Void> updateDeviceLocation(
            @PathVariable Long id,
            @RequestBody @Valid DeviceLocationUpdateRequest request){
        deviceService.updateDeviceLocation(id, request.locationId());
        return ResponseEntity.noContent().build();
    }


    // 장치 이름 수정 API - 전용 DTO 및 서비스 연동
    @PutMapping("/{id}/name")
    public ResponseEntity<Void> updateDeviceName(
            @PathVariable Long id,
            @RequestBody @Valid DeviceNameUpdateRequest request){
        deviceService.updateDeviceName(id, request.deviceName());
        return ResponseEntity.noContent().build();
    }


    // 개별 장지ID 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id){
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    // 전체 삭제
    @DeleteMapping
    public ResponseEntity<Void> deleteAllDevice(){
        deviceService.deleteAll();
        return ResponseEntity.noContent().build();
    }
}