package com.insighton.core.controller.api;

import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.dto.SensorUpdateRequest;
import com.insighton.core.domain.sensors.service.SensorService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sensor")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService; // 센서 서비스 주입


    // 단일 센서 조회 API (GET /api/v1/sensors/{id})
    @GetMapping("/{id}")
    public ResponseEntity<SensorResponse> getSensor(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("id") Long sensorId){
        // 기기 ID로 조회 후 반환
        SensorResponse responseDto = sensorService.getSensorById(userId, sensorId);
        return ResponseEntity.ok(responseDto);
    }

    // 통합 검색 API (GET /api/v1/sensors/search)
    @GetMapping("/search")
    public ResponseEntity<List<SensorResponse>> search(
            @RequestHeader("X-USER-ID") Long userid,
            @RequestParam Long groupId,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String eui,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String sensorName) {

        // 조건 검색 수행 후 리스트 반환
        List<SensorResponse> result = sensorService.searchSensors(userid, groupId, id, eui, locationId, sensorName);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateSensor(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long id,
            @RequestBody @Valid SensorUpdateRequest request
            ){
        // locationId 대신 locationName을 그대로 전달 - 서비스 계층에서 그룹 내 이름으로 위치를 찾음
        sensorService.updateSensor(userId, id, request.locationName(), request.sensorName());
        return ResponseEntity.noContent().build();
    }

    // 개별 장치 삭제 API (DELETE /api/v1/sensors/{id}/delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSensor(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long id){
        // 센서 단일 삭제 수행
        sensorService.deleteSensor(userId, id);
        return ResponseEntity.noContent().build();
    }

    // 전체 장치 삭제 API (DELETE /api/v1/sensors/deleteAll    ) - groupId 소속만 삭제
    @DeleteMapping
    public ResponseEntity<Void> deleteAllSensor(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam Long groupId){
        // 전체 센서 삭제 수행
        sensorService.deleteAll(userId, groupId);
        return ResponseEntity.noContent().build();
    }
}
