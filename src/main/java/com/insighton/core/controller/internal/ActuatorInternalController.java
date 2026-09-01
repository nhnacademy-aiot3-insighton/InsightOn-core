package com.insighton.core.controller.internal;

import com.insighton.core.controller.swagger.ActuatorInternalControllerApi;
import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogInternalResponse;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuatorrunlogs.service.ActuatorRunLogService;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.policy.ActuatorCommandPreset;
import com.insighton.core.domain.actuators.dto.ActuatorCommandRequest;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.exception.ActuatorLocationsActuatorTypeNotFound;
import com.insighton.core.domain.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.domain.actuators.exception.InvalidServiceCredentialException;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.usecase.actuator.UpdateActuatorStateByGroupUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 룰엔진/AI 등 신뢰된 내부 서비스 전용 API 모음 - 사용자용 ActuatorController와 완전히 분리
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1")
public class ActuatorInternalController implements ActuatorInternalControllerApi {

    private final ActuatorRunLogService actuatorRunLogService;
    private final UpdateActuatorStateByGroupUseCase updateActuatorStateByGroupUseCase;

    // AI 리포트 생성 배치 전용 - location 범위/기간별 액추에이터 실행 원본 로그 조회
    @Override
    @GetMapping("/actuators/run-logs")
    public ResponseEntity<List<ActuatorRunLogInternalResponse>> getRunLogs(
            @RequestParam List<Long> locationIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        return ResponseEntity.ok(actuatorRunLogService.getRunLogsForReport(locationIds, from, to));
    }

    // 룰엔진/AI 등 내부 시스템 전용 액추에이터 상태 변경
    @Override
    @PutMapping("/groups/{groupId}/locations/{locationId}/actuators/state")
    public ResponseEntity<Void> updateActuatorStateByGroup(
            @PathVariable Long groupId,
            @PathVariable Long locationId,
            @Valid @RequestBody ActuatorCommandRequest request) {

        updateActuatorStateByGroupUseCase.execute(groupId, locationId, request);
        return ResponseEntity.ok().build();
    }
}
