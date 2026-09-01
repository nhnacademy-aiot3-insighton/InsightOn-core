package com.insighton.core.usecase.actuator;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuators.dto.ActuatorCommandRequest;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.exception.ActuatorLocationsActuatorTypeNotFound;
import com.insighton.core.domain.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.domain.actuators.exception.InvalidServiceCredentialException;
import com.insighton.core.domain.actuators.policy.ActuatorCommandPreset;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 룰엔진/AI 등 내부 시스템 전용 - groupId+locationId 소유권 검증 후 같은 위치+타입 액추에이터 전체를 하나의 트랜잭션으로 상태 변경
@UseCase
@RequiredArgsConstructor
public class UpdateActuatorStateByGroupUseCase {

    private final LocationService locationService;
    private final ActuatorRepository actuatorRepository;
    private final ActuatorService actuatorService;

    @Transactional
    public void execute(Long groupId, Long locationId, ActuatorCommandRequest request) {

        // 이 내부 API는 시스템(AI/RuleEngine)만 호출 가능 - USER가 오면 즉시 차단
        if (request.callerService() == ExecutedByType.USER) {
            throw new InvalidServiceCredentialException("이 내부 API는 USER가 호출할 수 없습니다");
        }

        // groupId+locationId 소유권 검증 - 불일치/미존재면 여기서 LocationNotFoundException(404) 발생, 이후 조회/변경 전혀 안 탐
        locationService.getLocationByGroupId(locationId, groupId);

        // 문자열 actuatorType을 enum으로 변환, 존재하지 않는 값이면 400으로 처리
        ActuatorType actuatorType = parseActuatorType(request.actuatorType());

        // 요청받은 location+actuatorType 조합에 해당하는 액추에이터 전부 조회 (AI는 개별 actuatorId를 모르므로)
        List<Actuator> actuators = actuatorRepository.findByLocationLocationIdAndActuatorType(locationId, actuatorType);

        // 해당하는 액추에이터가 하나도 없으면 404로 명확히 구분 (AI가 "적용 안 됐다"를 알 수 있어야 함)
        if (actuators.isEmpty()) {
            throw new ActuatorLocationsActuatorTypeNotFound(locationId, actuatorType);
        }

        // 대상 액추에이터 전체에 상태 적용 (보통 1개, 같은 타입이 여러 대면 전부) - 메서드 전체가 하나의 트랜잭션이라 중간에 예외 나면 전부 롤백됨
        for (Actuator actuator : actuators) {
            // updateActuatorState는 병합이 아니라 통째 교체라 새 값 1개만 보내면 나머지(전원/모드/온도)가 날아가므로, 기존 상태를 복사해 그 위에 새 값만 얹어서 전달
            Map<String, Object> newState = new HashMap<>(
                    actuator.getCurrentState() != null ? actuator.getCurrentState() : Map.of());
            newState.put(request.command(), request.commandValue());

            ActuatorCommandPreset.validateCommandValues(actuatorType, newState);

            actuatorService.updateActuatorState(
                    groupId, actuator.getActuatorId(), newState, request.callerService(), null);
        }
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
