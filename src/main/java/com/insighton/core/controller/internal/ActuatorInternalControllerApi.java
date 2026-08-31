package com.insighton.core.controller.internal;

import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogInternalResponse;
import com.insighton.core.domain.actuators.dto.ActuatorCommandRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;

// Swagger 문서화 전용 인터페이스
@Tag(name = "Actuator (internal)", description = "룰엔진/AI 전용 액추에이터 내부 API")
public interface ActuatorInternalControllerApi {

    @Operation(summary = "위치/기간별 액추에이터 실행 로그 조회 (리포트용)")
    ResponseEntity<List<ActuatorRunLogInternalResponse>> getRunLogs(
            @Parameter(description = "위치 ID 목록") List<Long> locationIds,
            @Parameter(description = "조회 시작 시각") OffsetDateTime from,
            @Parameter(description = "조회 종료 시각") OffsetDateTime to);

    @Operation(summary = "시스템(AI/룰엔진) 전용 액추에이터 상태 변경")
    ResponseEntity<Void> updateActuatorStateBySystem(
            @Parameter(description = "위치 ID") Long locationId,
            @Valid ActuatorCommandRequest request);
}
