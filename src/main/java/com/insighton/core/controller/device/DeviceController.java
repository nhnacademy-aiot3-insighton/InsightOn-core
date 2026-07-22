package com.insighton.core.controller.device;

import com.insighton.core.dto.device.DeviceUpdateRequest;
import com.insighton.core.service.device.DeviceService;
import com.insighton.core.dto.device.DeviceResponseDto;
import com.insighton.core.dto.device.DeviceRequestDto;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    // 장치 등록 API
    // DeviceRequestDto를 받아 장치를 생성하고 생성된 ID를 반환
    @PostMapping
    public ResponseEntity<Long> createDevice(@RequestBody @Valid DeviceRequestDto requestDto){
        Long deviceId = deviceService.createDevice(requestDto);
        return ResponseEntity.ok(deviceId);
    }

    // 단일 deviceId 검색
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponseDto> getDevice(@PathVariable("id") Long deviceId){
        DeviceResponseDto responseDto = deviceService.searchDevices(deviceId,null,null,null,null).getFirst();
        return ResponseEntity.ok(responseDto);
    }


    // 통합 조건 검색 API
    @GetMapping("/search")
    public ResponseEntity<List<DeviceResponseDto>> search(
            //@RequestParam(required = false) 선택적으로 입력하도록 허용
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String eui,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long gatewayId,
            @RequestParam(required = false) String name) {

        // 파라미터 조건에 따라 적절한 데이터를 조회하여 반환
        List<DeviceResponseDto> result = deviceService.searchDevices(id, eui, locationId, gatewayId, name);
        return ResponseEntity.ok(result);
    }

    // 장치 위치 이동(수정) API
    @PatchMapping("/{id}/location")
    public ResponseEntity<Void> updateDeviceLocation(
            @PathVariable Long id,
            @RequestBody DeviceUpdateRequest request){

        // DeviceUpdateRequest에 포함된 위치ID를 추출하여 서비스로 넘김
        deviceService.updateDeviceLocation(id, request.locationId());
        // return ResponseEntity.noContent().build(); noContent 204설정
        // build는 응답 데이터를 넣지않고 이대로 포장함
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