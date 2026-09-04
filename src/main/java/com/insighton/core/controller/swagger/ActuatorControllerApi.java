package com.insighton.core.controller.swagger;

import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogResponse;
import com.insighton.core.domain.actuators.dto.ActuatorNameUpdateRequest;
import com.insighton.core.domain.actuators.dto.ActuatorRequest;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

// Swagger 문서화 전용 인터페이스
@Tag(name = "Actuator", description = "액추에이터 API")
public interface ActuatorControllerApi {

    @Operation(summary = "액추에이터 생성")
    ResponseEntity<Long> createActuator(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "그룹 ID") Long groupsId,
            @Valid ActuatorRequest request);

    @Operation(summary = "단일 액추에이터 조회")
    ResponseEntity<ActuatorResponse> getActuatorById(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "그룹 ID") Long groupsId,
            @Parameter(description = "액추에이터 ID") Long actuatorId);

    @Operation(summary = "위치별 액추에이터 목록 조회")
    ResponseEntity<List<ActuatorResponse>> getActuatorsByLocationId(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "그룹 ID") Long groupsId,
            @Parameter(description = "위치 ID") Long locationId);

    @Operation(summary = "액추에이터 상태 변경")
    ResponseEntity<Void> updateActuatorState(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "그룹 ID") Long groupsId,
            @Parameter(description = "액추에이터 ID") Long actuatorId,
            @Parameter(description = "변경할 상태 값") Map<String, Object> newState);

    @Operation(summary = "액추에이터 실행 이력 조회")
    ResponseEntity<Page<ActuatorRunLogResponse>> getActuatorRunLogs(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "그룹 ID") Long groupsId,
            @Parameter(description = "액추에이터 ID") Long actuatorId,
            Pageable pageable);

    @Operation(summary = "액추에이터 이름 수정")
    ResponseEntity<Void> updateActuatorName(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "그룹 ID") Long groupsId,
            @Parameter(description = "액추에이터 ID") Long actuatorId,
            @Valid ActuatorNameUpdateRequest request);

    @Operation(summary = "액추에이터 삭제")
    ResponseEntity<Void> deleteActuatorById(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "그룹 ID") Long groupsId,
            @Parameter(description = "액추에이터 ID") Long actuatorId);

    @Operation(summary = "그룹 전체 액추에이터 삭제")
    ResponseEntity<Void> deleteAll(
            @Parameter(description = "사용자 ID") Long userId,
            @Parameter(description = "그룹 ID") Long groupsId);
}
