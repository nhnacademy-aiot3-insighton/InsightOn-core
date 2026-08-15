package com.insighton.core.controller.internal;

import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogInternalResponse;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuatorrunlogs.service.ActuatorRunLogService;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.policy.ActuatorCommandPreset;
import com.insighton.core.domain.actuators.dto.ActuatorCommandRequest;
import com.insighton.core.domain.actuators.exception.ActuatorLocationsActuatorTypeNotFound;
import com.insighton.core.domain.actuators.exception.InvalidServiceCredentialException;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import com.insighton.core.domain.actuators.service.ActuatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

// 룰엔진/AI 등 신뢰된 내부 서비스 전용 API 모음 - 사용자용 ActuatorController와 완전히 분리
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class ActuatorInternalController {


    private final ActuatorRunLogService actuatorRunLogService;
    private final ActuatorService actuatorService;
    private final ActuatorRepository actuatorRepository;

    // AI 리포트 생성 배치 전용 - location 범위/기간별 액추에이터 실행 원본 로그 조회
    @GetMapping("/actuators/run-logs")
    public ResponseEntity<List<ActuatorRunLogInternalResponse>> getRunLogs(
            @RequestParam List<Long> locationIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        return ResponseEntity.ok(actuatorRunLogService.getRunLogsForReport(locationIds, from, to));
    }



    // 룰엔진/AI 등 내부 시스템 전용 액추에이터 상태 변경 - 실제 조작(쓰기)이므로 인증 필수 유지
    @PutMapping("/locations/{location-id}/actuators/state")
    public ResponseEntity<Void> updateActuatorStateBySystem(
            @PathVariable("location-id") Long locationId,
            @Valid @RequestBody ActuatorCommandRequest request) {

        // 이 내부 API는 시스템(AI/RuleEngine)만 호출 가능 - USER가 오면 즉시 차단
        if(request.callerService() == ExecutedByType.USER){
            throw new InvalidServiceCredentialException("이 내부 API는 USER가 호출할 수 없습니다");
        }

        // 액추에이터 종류
        String actuatorType = request.actuatorType();

        // 요청받은 location+actuatorType 조합에 해당하는 액추에이터 전부 조회 (AI는 개별 actuatorId를 모르므로)
        List<Actuator> actuators = actuatorRepository.findByLocationLocationIdAndActuatorType(locationId, actuatorType);

        // 해당하는 액추에이터가 하나도 없으면 404로 명확히 구분 (AI가 "적용 안 됐다"를 알 수 있어야 함)
        if(actuators.isEmpty()){
            throw new ActuatorLocationsActuatorTypeNotFound(locationId, actuatorType);
        }

        // 요청받은 명령/값을 적용할 상태 맵으로 조립
        Map<String, Object> newState = Map.of(request.command(), request.commandValue());

        // 검증 먼저
        ActuatorCommandPreset.validateCommandValues(actuatorType, newState);

        // 검증 통과 후 대상 액추에이터 전체에 동일하게 상태 적용 (보통 1개, 같은 타입이 여러 대면 전부)
        for (Actuator actuator : actuators) {
            actuatorService.updateActuatorState(
                    null, actuator.getActuatorId(), newState, request.callerService(), null);
        }

        return ResponseEntity.ok().build();
    }

}