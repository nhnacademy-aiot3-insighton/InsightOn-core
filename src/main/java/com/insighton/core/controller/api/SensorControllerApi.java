package com.insighton.core.controller.api;

import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.dto.SensorUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;

// Swagger 문서화 전용 인터페이스
@Tag(name = "Sensor", description = "센서 API")
public interface SensorControllerApi {

    @Operation(summary = "단일 센서 조회")
    ResponseEntity<SensorResponse> getSensor(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "센서 ID") Long sensorId);

    @Operation(summary = "센서 검색")
    ResponseEntity<List<SensorResponse>> search(
            @Parameter(description = "사용자 ID") Long userid,
            @Parameter(description = "그룹 ID") Long groupId,
            @Parameter(description = "센서 ID") Long id,
            @Parameter(description = "센서 EUI") String eui,
            @Parameter(description = "위치 ID") Long locationId,
            @Parameter(description = "센서 이름") String sensorName);

    @Operation(summary = "장소 미배정 센서 목록 조회")
    ResponseEntity<List<SensorResponse>> getUnassignedSensors(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "그룹 ID") Long groupId);

    @Operation(summary = "센서 수정")
    ResponseEntity<Void> updateSensor(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "센서 ID") Long sensorId,
            @Valid SensorUpdateRequest request);

    @Operation(summary = "센서 삭제")
    ResponseEntity<Void> deleteSensor(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "센서 ID") Long sensorId);

    @Operation(summary = "그룹 전체 센서 삭제")
    ResponseEntity<Void> deleteAllSensor(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "그룹 ID") Long groupId);
}
