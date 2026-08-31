package com.insighton.core.controller.api;

import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.dto.SensorUpdateRequest;
import com.insighton.core.usecase.sensor.DeleteAllSensorUseCase;
import com.insighton.core.usecase.sensor.DeleteSensorUseCase;
import com.insighton.core.usecase.sensor.GetSensorUseCase;
import com.insighton.core.usecase.sensor.GetUnassignedSensorsUseCase;
import com.insighton.core.usecase.sensor.SearchSensorUseCase;
import com.insighton.core.usecase.sensor.UpdateSensorUseCase;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sensor")
@RequiredArgsConstructor
public class SensorController implements SensorControllerApi {

    private final GetSensorUseCase getSensorUseCase;
    private final SearchSensorUseCase searchSensorUseCase;
    private final GetUnassignedSensorsUseCase getUnassignedSensorsUseCase;
    private final UpdateSensorUseCase updateSensorUseCase;
    private final DeleteSensorUseCase deleteSensorUseCase;
    private final DeleteAllSensorUseCase deleteAllSensorUseCase;


    // 단일 센서 조회 API (GET /api/v1/sensors/{id})
    @Override
    @GetMapping("/{id}")
    public ResponseEntity<SensorResponse> getSensor(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("id") Long sensorId){
        // 기기 ID로 조회 후 반환
        SensorResponse responseDto = getSensorUseCase.getSensorById(userId, sensorId);
        return ResponseEntity.ok(responseDto);
    }

    // 통합 검색 API (GET /api/v1/sensors/search)
    @Override
    @GetMapping("/search")
    public ResponseEntity<List<SensorResponse>> search(
            @RequestHeader("X-USER-ID") Long userid,
            @RequestParam Long groupId,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String eui,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String sensorName) {

        // 조건 검색 수행 후 리스트 반환
        List<SensorResponse> result = searchSensorUseCase.searchSensors(userid, groupId, id, eui,
                new SensorUpdateRequest(locationId, sensorName));
        return ResponseEntity.ok(result);
    }

    // 장소 미배정 센서 목록 조회 API (GET /api/v1/sensor/unassigned) - autoProvision으로 자동 등록만 되고
    // 아직 위치가 안 정해진 센서들을 관리자가 찾아서 배치할 수 있게 별도 API로 분리
    @Override
    @GetMapping("/unassigned")
    public ResponseEntity<List<SensorResponse>> getUnassignedSensors(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam Long groupId) {
        List<SensorResponse> result = getUnassignedSensorsUseCase.getUnassignedSensors(userId, groupId);
        return ResponseEntity.ok(result);
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateSensor(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("id") Long sensorId,
            @RequestBody @Valid SensorUpdateRequest request
            ){
        updateSensorUseCase.updateSensor(userId, sensorId, request);
        return ResponseEntity.noContent().build();
    }

    // 개별 장치 삭제 API (DELETE /api/v1/sensors/{id}/delete)
    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSensor(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("id") Long sensorId){
        // 센서 단일 삭제 수행
        deleteSensorUseCase.deleteSensor(userId, sensorId);
        return ResponseEntity.noContent().build();
    }

    // 전체 장치 삭제 API (DELETE /api/v1/sensors/deleteAll    ) - groupId 소속만 삭제
    @Override
    @DeleteMapping
    public ResponseEntity<Void> deleteAllSensor(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam Long groupId){
        // 전체 센서 삭제 수행
        deleteAllSensorUseCase.deleteAll(userId, groupId);
        return ResponseEntity.noContent().build();
    }
}
