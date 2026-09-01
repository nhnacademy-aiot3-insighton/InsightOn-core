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
    // 구버전 무그룹 엔드포인트 전용 의존성 - Engine/AI 전환 완료 후 이 필드들과 아래 구버전 메서드 통째로 제거 예정
    private final ActuatorService actuatorService;
    private final ActuatorRepository actuatorRepository;

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

    // [Deprecated] 그룹 소유권 검증 없는 구버전 - Engine/AI가 신규 API로 전환 완료할 때까지만 임시로 유지, 전환 확인되면 이 메서드 통째로 제거
    @Override
    @PutMapping("/locations/{location-id}/actuators/state")
    public ResponseEntity<Void> updateActuatorStateBySystem(
            @PathVariable("location-id") Long locationId,
            @Valid @RequestBody ActuatorCommandRequest request) {

        // 이 내부 API는 시스템(AI/RuleEngine)만 호출 가능 - USER가 오면 즉시 차단
        if (request.callerService() == ExecutedByType.USER) {
            throw new InvalidServiceCredentialException("이 내부 API는 USER가 호출할 수 없습니다");
        }

        // 문자열 actuatorType을 enum으로 변환, 존재하지 않는 값이면 400으로 처리
        ActuatorType actuatorType = parseActuatorType(request.actuatorType());

        // 요청받은 location+actuatorType 조합에 해당하는 액추에이터 전부 조회 (AI는 개별 actuatorId를 모르므로)
        List<Actuator> actuators = actuatorRepository.findByLocationLocationIdAndActuatorType(locationId, actuatorType);

        // 해당하는 액추에이터가 하나도 없으면 404로 명확히 구분 (AI가 "적용 안 됐다"를 알 수 있어야 함)
        if (actuators.isEmpty()) {
            throw new ActuatorLocationsActuatorTypeNotFound(locationId, actuatorType);
        }

        // 대상 액추에이터 전체에 상태 적용 (보통 1개, 같은 타입이 여러 대면 전부)
        for (Actuator actuator : actuators) {
            // updateActuatorState는 병합이 아니라 통째 교체라 새 값 1개만 보내면 나머지(전원/모드/온도)가 날아가므로, 기존 상태를 복사해 그 위에 새 값만 얹어서 전달
            Map<String, Object> newState = new HashMap<>(
                    actuator.getCurrentState() != null ? actuator.getCurrentState() : Map.of());
            newState.put(request.command(), request.commandValue());

            ActuatorCommandPreset.validateCommandValues(actuatorType, newState);

            actuatorService.updateActuatorState(
                    null, actuator.getActuatorId(), newState, request.callerService(), null);
        }

        return ResponseEntity.ok().build();
    }

    // ActuatorType.valueOf()가 정의 안 된 값(오타 등)을 받으면 IllegalArgumentException을 던지는데,
    // 이건 GlobalExceptionHandler에 등록 안 되어있어서 그대로 두면 500으로 나감 -> 400으로 변환
    private ActuatorType parseActuatorType(String actuatorType) {
        try {
            return ActuatorType.valueOf(actuatorType); // 유효하지 않는 문자열이면 여기서 예외 발생
        } catch (IllegalArgumentException e) {
            throw new InvalidActuatorValueException("알 수 없는 actuatorType입니다: " + actuatorType);
        }
    }
}
