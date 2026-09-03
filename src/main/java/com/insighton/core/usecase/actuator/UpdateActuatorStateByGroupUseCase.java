package com.insighton.core.usecase.actuator;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuators.dto.ActuatorCommandRequest;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.exception.ActuatorLocationsActuatorTypeNotFound;
import com.insighton.core.domain.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.domain.actuators.exception.InvalidServiceCredentialException;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import com.insighton.core.domain.location.service.LocationService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

// 룰엔진/AI 등 내부 시스템 전용 - groupId+locationId 소유권 검증 후 같은 위치+타입 액추에이터 전체를 상태 변경
@UseCase
@RequiredArgsConstructor
public class UpdateActuatorStateByGroupUseCase {

    private final LocationService locationService;
    private final ActuatorRepository actuatorRepository;
    private final ActuatorControlFacade actuatorControlFacade;

    public void execute(Long groupId, Long locationId, ActuatorCommandRequest request) {

        // 이 내부 API는 시스템(AI/RuleEngine)만 호출 가능 - USER가 오면 즉시 차단
        if (request.callerService() == ExecutedByType.USER) {
            throw new InvalidServiceCredentialException("이 내부 API는 USER가 호출할 수 없습니다");
        }

        // groupId+locationId 소유권 검증 - 불일치/미존재면 여기서 LocationNotFoundException(404) 발생
        locationService.getLocationByGroupId(locationId, groupId);

        // 문자열 actuatorType을 enum으로 변환, 존재하지 않는 값이면 400으로 처리
        ActuatorType actuatorType = parseActuatorType(request.actuatorType());

        // 요청받은 location+actuatorType 조합에 해당하는 액추에이터 전부 조회 (AI는 개별 actuatorId를 모르므로)
        List<Actuator> actuators = actuatorRepository.findByLocationLocationIdAndActuatorType(locationId, actuatorType);

        // 해당하는 액추에이터가 하나도 없으면 404로 명확히 구분 (AI가 "적용 안 됐다"를 알 수 있어야 함)
        if (actuators.isEmpty()) {
            throw new ActuatorLocationsActuatorTypeNotFound(locationId, actuatorType);
        }

        // 상태 병합, 명령 검증, 공급자 Adapter 호출, 성공 후 저장은 Facade가 담당.
        // @Transactional 없음 - Facade 안에서 외부 호출 후 저장이라 감쌀 수 없고, 다중 장치는 fail-fast (플랜 §14)
        Map<String, Object> partialState = Map.<String, Object>of(request.command(), request.commandValue());
        for (Actuator actuator : actuators) {
            actuatorControlFacade.control(
                    groupId, actuator.getActuatorId(), partialState, request.callerService(), null);
        }
    }

    // ActuatorType.valueOf()가 정의 안 된 값을 받으면 IllegalArgumentException을 던지는데,
    // GlobalExceptionHandler에 등록 안 되어있어서 그대로 두면 500 -> 400으로 변환
    private ActuatorType parseActuatorType(String actuatorType) {
        try {
            return ActuatorType.valueOf(actuatorType);
        } catch (IllegalArgumentException e) {
            throw new InvalidActuatorValueException("알 수 없는 actuatorType입니다: " + actuatorType);
        }
    }
}
