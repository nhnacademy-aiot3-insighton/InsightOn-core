package com.insighton.core.controller.api;

import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogResponse;
import com.insighton.core.domain.actuators.dto.ActuatorNameUpdateRequest;
import com.insighton.core.domain.actuators.dto.ActuatorRequest;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

// Swagger 문서화 애노테이션 전용 인터페이스 - 실제 스프링 바인딩 애노테이션은 구현체(ActuatorController)에만 남김
@Tag(name = "Actuator", description = "액추에이터 생성/조회/제어/삭제 및 실행 이력 API")
public interface ActuatorControllerApi {

    @Operation(summary = "액추에이터 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공, 생성된 actuatorId 반환"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 위치")
    })
    ResponseEntity<Long> createActuator(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupsId,
            @Valid ActuatorRequest request);

    @Operation(summary = "단일 액추에이터 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 다른 그룹 소속인 액추에이터")
    })
    ResponseEntity<ActuatorResponse> getActuatorById(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupsId,
            @Parameter(description = "액추에이터 ID", required = true) Long actuatorId);

    @Operation(summary = "위치별 액추에이터 목록 조회")
    ResponseEntity<List<ActuatorResponse>> getActuatorsByLocationId(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupsId,
            @Parameter(description = "위치 ID", required = true) Long locationId);

    @Operation(summary = "액추에이터 상태 변경", description = "사용자가 직접 제어 명령을 실행합니다. 실행 이력이 함께 기록됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "400", description = "상태 값이 비어있음")
    })
    ResponseEntity<Void> updateActuatorState(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupsId,
            @Parameter(description = "액추에이터 ID", required = true) Long actuatorId,
            @Parameter(description = "변경할 상태 값 (예: {\"power\": \"ON\"})", required = true) Map<String, Object> newState);

    @Operation(summary = "액추에이터 실행 이력 조회", description = "페이지네이션으로 실행 이력을 조회합니다.")
    ResponseEntity<Page<ActuatorRunLogResponse>> getActuatorRunLogs(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupsId,
            @Parameter(description = "액추에이터 ID", required = true) Long actuatorId,
            Pageable pageable);

    @Operation(summary = "액추에이터 이름 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "빈 이름")
    })
    ResponseEntity<Void> updateActuatorName(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupsId,
            @Parameter(description = "액추에이터 ID", required = true) Long actuatorId,
            @Valid ActuatorNameUpdateRequest request);

    @Operation(summary = "액추에이터 삭제")
    ResponseEntity<Void> deleteActuatorById(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupsId,
            @Parameter(description = "액추에이터 ID", required = true) Long actuatorId);

    @Operation(summary = "그룹 전체 액추에이터 삭제")
    ResponseEntity<Void> deleteAll(
            @Parameter(description = "요청 사용자 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupsId);
}
