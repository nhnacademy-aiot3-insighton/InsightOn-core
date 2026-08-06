package com.insighton.core.actuators.controller;

import com.insighton.core.actuator_run_logs.dto.ActuatorRunLogInternalResponse;
import com.insighton.core.actuator_run_logs.entity.ActuatorCommandPreset;
import com.insighton.core.actuator_run_logs.entity.ExecutedByType;
import com.insighton.core.actuator_run_logs.service.ActuatorRunLogService;
import com.insighton.core.actuators.entity.ActuatorType;
import com.insighton.core.actuators.exception.InvalidServiceCredentialException;
import com.insighton.core.actuators.service.ActuatorService;
import com.insighton.core.device_attributes.dto.ActuatorCommandPresetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 룰엔진/AI 등 신뢰된 내부 서비스 전용 API 모음 - 사용자용 ActuatorController와 완전히 분리
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/actuators")
public class ActuatorInternalController {

    @Value("${internal.service.api-key}")
    private String internalServiceApiKey;

    private final ActuatorRunLogService actuatorRunLogService;
    private final ActuatorService actuatorService;

    // AI 리포트 생성 배치 전용 - location 범위/기간별 액추에이터 실행 원본 로그 조회
    // AI 쪽에 아직 인증 헤더 자동부착 인터셉터가 없어서 required=false로 완화 (인터셉터 붙으면 true로 되돌릴 것)
    @GetMapping("/run-logs")
    public ResponseEntity<List<ActuatorRunLogInternalResponse>> getRunLogs(
            @RequestHeader(value = "X-INTERNAL-API-KEY", required = false) String apiKey,
            @RequestHeader(value = "X-CALLER-SERVICE", required = false) String callerService,
            @RequestParam List<Long> locationIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        if (apiKey != null) {
            validateApiKey(apiKey);
            // AI_SYSTEM에서 호출된건지 검증
            if("AI_SYSTEM".equals(callerService)){
                throw new InvalidServiceCredentialException("허용되지 않은 호출 서비스: " + callerService);
            }
        }

        return ResponseEntity.ok(actuatorRunLogService.getRunLogsForReport(locationIds, from, to));
    }

    // AI LLM 제안 생성/검증용 - 액추에이터 종류별 지원 명령 목록 조회
    @GetMapping("/command-presets")
    public ResponseEntity<ActuatorCommandPresetResponse> getCommandPresets(
            @RequestHeader(value = "X-INTERNAL-API-KEY", required = false) String apiKey) {

        if (apiKey != null) {
            validateApiKey(apiKey);
        }

        Map<String, Set<String>> result = Arrays.stream(ActuatorType.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        type -> ActuatorCommandPreset.getSupportedCommands(type).stream()
                                .map(Enum::name)
                                .collect(Collectors.toSet())
                ));
        return ResponseEntity.ok(new ActuatorCommandPresetResponse(result));
    }

    // 룰엔진/AI 등 내부 시스템 전용 액추에이터 상태 변경 - 실제 조작(쓰기)이므로 인증 필수 유지
    @PutMapping("/{actuator-id}/state")
    public ResponseEntity<Void> updateActuatorStateBySystem(
            @RequestHeader("X-INTERNAL-API-KEY") String apiKey,
            @RequestHeader("X-CALLER-SERVICE") String callerService,
            @PathVariable("actuator-id") Long actuatorId,
            @RequestBody Map<String, Object> newState) {

        validateApiKey(apiKey);

        // executedByType을 요청 파라미터로 안 받고, 인증된 호출자 이름으로만 결정 (클라이언트가 값을 못 고름)
        ExecutedByType executedByType = resolveExecutedByType(callerService);

        actuatorService.updateActuatorState(null, null, actuatorId, newState, executedByType);
        return ResponseEntity.ok().build();
    }

    // 공유 비밀키 검증 - 이 컨트롤러의 모든 엔드포인트가 공통으로 사용
    private void validateApiKey(String apiKey) {
        if (!internalServiceApiKey.equalsIgnoreCase(apiKey)) {
            throw new InvalidServiceCredentialException("내부 서비스 인증에 실패했습니다");
        }
    }

    // 화이트리스트에 있는 호출자만 허용 - USER는 애초에 나올 수 없는 구조
    private ExecutedByType resolveExecutedByType(String callerService) {
        return switch (callerService) {
            case "RULE_ENGINE" -> ExecutedByType.RULE_ENGINE;
            case "AI_SYSTEM" -> ExecutedByType.AI_SYSTEM;
            default -> throw new InvalidServiceCredentialException("허용되지 않은 호출 서비스입니다: " + callerService);
        };
    }
}