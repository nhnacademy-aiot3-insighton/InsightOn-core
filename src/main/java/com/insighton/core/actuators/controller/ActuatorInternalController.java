package com.insighton.core.actuators.controller;

import com.insighton.core.actuatorrunlogs.dto.ActuatorRunLogInternalResponse;
import com.insighton.core.actuatorrunlogs.entity.CommandType;
import com.insighton.core.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.actuators.entity.Actuator;
import com.insighton.core.actuators.dto.ActuatorCommandPreset;
import com.insighton.core.actuators.dto.ActuatorCommandRequest;
import com.insighton.core.actuatorrunlogs.service.ActuatorRunLogService;
import com.insighton.core.actuators.entity.ActuatorType;
import com.insighton.core.actuators.exception.ActuatorLocationsActuatorTypeNotFound;
import com.insighton.core.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.actuators.exception.InvalidServiceCredentialException;
import com.insighton.core.actuators.repository.ActuatorRepository;
import com.insighton.core.actuators.service.ActuatorService;
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
    // AI 쪽에 아직 인증 헤더 자동부착 인터셉터가 없어서 required=false로 완화 (인터셉터 붙으면 true로 되돌릴 것)
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
            @RequestBody ActuatorCommandRequest request) {

        if(request.callerService() == ExecutedByType.USER){
            throw new InvalidServiceCredentialException("이 내부 API는 USER가 호출할 수 없습니다");
        }

        ActuatorType actuatorType = ActuatorType.valueOf(request.actuatorType());

        List<Actuator> actuators = actuatorRepository.findByLocationLocationIdAndActuatorType(locationId, actuatorType);

        if(actuators.isEmpty()){
            throw new ActuatorLocationsActuatorTypeNotFound(locationId, actuatorType);
        }

        Map<String, Object> newState = Map.of(request.command(), request.commandValue());

        // 액추에이터 타입의 명령이 맞는지 확인용도
        for(Actuator actuator : actuators){
            validateCommandValues(actuatorType, newState);
        }
        // 위의 검증이 확인이 되면 업데이트
        for (Actuator actuator : actuators) {
            actuatorService.updateActuatorState(null, null, actuator.getActuatorId(), newState, request.callerService());
        }

        return ResponseEntity.ok().build();
    }

    // newState의 각 키/값이 이 액추에이터 타입에서 실제로 허용되는 명령/값인지 검증
    private void validateCommandValues(ActuatorType actuatorType, Map<String, Object> newState) {
        newState.forEach((key, value) -> {
            CommandType commandType = CommandType.fromStateKey(key)
                    .orElseThrow(() -> new InvalidActuatorValueException("알 수 없는 제어 명령키: " + key));
            String stringValue = String.valueOf(value);
            if (!ActuatorCommandPreset.isValidValue(actuatorType, commandType, stringValue)) {
                throw new InvalidActuatorValueException(
                        "허용되지 않은 명령 값입니다. (commandType=" + commandType + ", value=" + stringValue + ")");
            }
        });
    }

}