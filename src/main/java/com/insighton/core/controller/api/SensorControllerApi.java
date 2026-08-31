package com.insighton.core.controller.api;

import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.dto.SensorUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;

// Swagger 문서화 애노테이션 전용 인터페이스 - 실제 스프링 바인딩 애노테이션(@RequestHeader 등)은
// 구현체(SensorController)에만 남기고, 여긴 문서 관련 애노테이션만 둬서 컨트롤러를 깔끔하게 유지함
@Tag(name = "Sensor", description = "센서 조회/검색/수정/삭제 API")
public interface SensorControllerApi {

    @Operation(summary = "단일 센서 조회", description = "센서 ID로 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 센서")
    })
    ResponseEntity<SensorResponse> getSensor(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "센서 ID", required = true) Long sensorId);

    @Operation(summary = "센서 통합 검색",
            description = "id/eui/locationId/sensorName 중 있는 조건만 AND로 조합해서 검색합니다. groupId는 필수입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "groupId 누락")
    })
    ResponseEntity<List<SensorResponse>> search(
            @Parameter(description = "요청 사용자 ID", required = true) Long userid,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "센서 PK로 정확히 일치 검색") Long id,
            @Parameter(description = "센서 EUI") String eui,
            @Parameter(description = "위치 ID") Long locationId,
            @Parameter(description = "센서 이름") String sensorName);

    @Operation(summary = "장소 미배정 센서 목록 조회",
            description = "autoProvision으로 자동 등록만 되고 아직 위치가 안 정해진 센서 목록을 조회합니다.")
    ResponseEntity<List<SensorResponse>> getUnassignedSensors(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId);

    @Operation(summary = "센서 수정", description = "locationId 또는 sensorName 중 있는 값만 반영됩니다. 최소 하나는 필요합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "변경할 값이 없음"),
            @ApiResponse(responseCode = "404", description = "센서 또는 위치를 찾을 수 없음")
    })
    ResponseEntity<Void> updateSensor(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "센서 ID", required = true) Long sensorId,
            @Valid SensorUpdateRequest request);

    @Operation(summary = "센서 단일 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 센서")
    })
    ResponseEntity<Void> deleteSensor(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "센서 ID", required = true) Long sensorId);

    @Operation(summary = "그룹 전체 센서 삭제", description = "groupId 소속 센서를 전부 삭제합니다.")
    ResponseEntity<Void> deleteAllSensor(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId);
}
