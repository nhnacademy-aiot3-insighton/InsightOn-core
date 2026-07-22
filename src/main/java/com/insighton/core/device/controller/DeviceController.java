package com.insighton.core.device.controller;

import com.insighton.core.device.dto.DeviceUpdateRequest;
import com.insighton.core.device.service.DeviceService;
import com.insighton.core.device.dto.DeviceResponseDto;
import com.insighton.core.device.dto.DeviceRequestDto;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import java.util.List;

/**
 * IoT 장치(Device) 생성, 조회, 수정, 삭제(CRUD) REST API 컨트롤러.
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 신규 장치 등록 API
     *
     * @param requestDto 등록할 장치 상세 정보 (@Valid를 통해 필드 유효성 검증)
     * @return 생성된 장치 ID (HTTP 200 OK)
     */
    @PostMapping
    public ResponseEntity<Long> createDevice(@RequestBody @Valid DeviceRequestDto requestDto){
        Long deviceId = deviceService.createDevice(requestDto);
        return ResponseEntity.ok(deviceId);
    }

    /**
     * 단일 장치 상세 조회 API
     *
     * @param deviceId 조회할 장치 ID
     * @return 조회된 장치 상세 정보 (HTTP 200 OK)
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponseDto> getDevice(@PathVariable("id") Long deviceId){
        DeviceResponseDto responseDto = deviceService.searchDevices(deviceId, null, null, null, null).getFirst();
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 통합 조건 검색 API
     * <p>
     * ID, EUI, 위치, 게이트웨이, 이름 중 원하는 조건을 선택적 쿼리 파라미터로 전달하여 조건 검색을 실행합니다.
     *
     * @param id 장치 ID (선택)
     * @param eui DevEUI (선택)
     * @param locationId 위치 ID (선택)
     * @param gatewayId 게이트웨이 ID (선택)
     * @param name 장치 이름 (선택)
     * @return 조건에 일치하는 장치 리스트 (HTTP 200 OK)
     */
    @GetMapping("/search")
    public ResponseEntity<List<DeviceResponseDto>> search(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String eui,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long gatewayId,
            @RequestParam(required = false) String name) {

        List<DeviceResponseDto> result = deviceService.searchDevices(id, eui, locationId, gatewayId, name);
        return ResponseEntity.ok(result);
    }

    /**
     * 장치 위치 수정(이동) API
     *
     * @param id 수정 대상 장치 ID
     * @param request 변경할 위치 ID를 담은 DTO
     * @return 응답 본문 없는 성공 응답 (HTTP 204 No Content)
     */
    @PatchMapping("/{id}/location")
    public ResponseEntity<Void> updateDeviceLocation(
            @PathVariable Long id,
            @RequestBody DeviceUpdateRequest request){

        deviceService.updateDeviceLocation(id, request.locationId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 단일 장치 삭제 API
     *
     * @param id 삭제할 장치 ID
     * @return 응답 본문 없는 성공 응답 (HTTP 204 No Content)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id){
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 전체 장치 일괄 삭제 API
     *
     * @return 응답 본문 없는 성공 응답 (HTTP 204 No Content)
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllDevice(){
        deviceService.deleteAll();
        return ResponseEntity.noContent().build();
    }
}