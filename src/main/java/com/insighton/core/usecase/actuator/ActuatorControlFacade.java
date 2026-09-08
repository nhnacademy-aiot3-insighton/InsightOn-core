package com.insighton.core.usecase.actuator;

import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuators.control.ActuatorControlAdapter;
import com.insighton.core.domain.actuators.control.ActuatorControlAdapterRegistry;
import com.insighton.core.domain.actuators.control.ActuatorControlCommand;
import com.insighton.core.domain.actuators.control.ActuatorControlResult;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.exception.ActuatorNotFoundException;
import com.insighton.core.domain.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.domain.actuators.policy.ActuatorCommandPreset;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.location.repository.LocationRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 액추에이터 제어 공통 흐름 - 조회, 상태 병합, 검증, 공급자 Adapter 호출, 성공 후 저장까지 한 곳에서 처리.
// UpdateActuatorStateUseCase(사용자)와 ActuatorInternalController(시스템) 둘 다 여기를 통해서만 제어한다.
@Service
@Slf4j
@RequiredArgsConstructor
public class ActuatorControlFacade {

    private final ActuatorRepository actuatorRepository;
    private final LocationRepository locationRepository; // 소유권 교차검증용 - ActuatorServiceImpl과 동일한 방식
    private final ActuatorControlAdapterRegistry adapterRegistry;
    private final ActuatorService actuatorService;

    public void control(Long groupsId, Long actuatorId, Map<String, Object> partialState,
                         ExecutedByType executedByType, Long actingUserId) {

        if (partialState == null || partialState.isEmpty()) {
            throw new InvalidActuatorValueException("액추에이터 제어 상태 값(newState)은 비어있음");
        }

        Actuator actuator = actuatorRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException(actuatorId));

        // 외부로 명령 나가기 전에 소유권부터 확인 - Adapter 호출 뒤로 미루면 권한 없는 요청도
        // 실제 장치/시뮬레이터에 명령이 먼저 나가버림
        if (executedByType == ExecutedByType.USER) {
            boolean belongsToGroup = locationRepository
                    .findByLocationIdAndGroupGroupId(actuator.getLocation().getLocationId(), groupsId)
                    .isPresent();
            if (!belongsToGroup) {
                throw new ActuatorNotFoundException(actuatorId);
            }
        }

        if (actuator.getControlProvider() == null || actuator.getExternalDeviceId() == null) {
            throw new InvalidActuatorValueException("제어 공급자가 연결되지 않은 액추에이터입니다 (actuatorId=" + actuatorId + ")");
        }

        // 기존 상태에 요청받은 키만 덮어쓰기 (부분 병합)
        Map<String, Object> mergedState = new HashMap<>(
                actuator.getCurrentState() != null ? actuator.getCurrentState() : Map.of());
        mergedState.putAll(partialState);

        ActuatorCommandPreset.validateCommandValues(actuator.getActuatorType(), mergedState);

        ActuatorControlAdapter adapter = adapterRegistry.get(actuator.getControlProvider()); // 제품 종류 판단
        log.info("액추에이터 {} ({}) 제어 - provider={}, adapter={}, externalDeviceId={}",
                actuatorId, actuator.getActuatorType(), actuator.getControlProvider(),
                adapter.getClass().getSimpleName(), actuator.getExternalDeviceId());

        ActuatorControlCommand command = new ActuatorControlCommand(
                actuator.getExternalDeviceId(), actuator.getActuatorType(), mergedState);

        // Adapter가 예외를 던지면(공급자 실패) 여기서 전파되고 아래 저장은 실행 안 됨
        ActuatorControlResult result = adapter.control(command);

        // 공급자 성공 응답을 받은 후에만 CORE 상태·실행 로그 반영
        actuatorService.updateActuatorState(groupsId, actuatorId, result.appliedState(), executedByType, actingUserId);
    }
}
