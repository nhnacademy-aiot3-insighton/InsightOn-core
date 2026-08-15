package com.insighton.core.domain.actuators.service.impl;

import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuatorrunlogs.repository.ActuatorRunLogRepository;
import com.insighton.core.domain.actuatorrunlogs.service.ActuatorRunLogService;
import com.insighton.core.domain.actuators.dto.ActuatorRequest;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.exception.ActuatorNotFoundException;
import com.insighton.core.domain.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.actuatortypedefinition.exception.ActuatorTypeNotFoundException;
import com.insighton.core.domain.actuatortypedefinition.repository.ActuatorTypeRepository;
import com.insighton.core.domain.location.dto.response.LocationListResponse;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import com.insighton.core.domain.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActuatorServiceImpl implements ActuatorService {

    private final LocationRepository locationsRepository; // 소유권 교차검증용
    private final ActuatorRepository actuatorRepository; // 액추에이터 조회/저장
    private final ActuatorRunLogService actuatorRunLogService; // 제어 이력 기록용
    private final ActuatorRunLogRepository actuatorRunLogRepository;
    private final ActuatorTypeRepository actuatorTypeRepository; // 생성 시 actuatorType 존재 검증

    // 대상 액추에이터가 실제로 groupsId 소속인지 확인 (locationId -> location.groupId 교차검증)
    // 다른 그룹 리소스 존재 여부를 노출하지 않기 위해 404로 통일
    private void validateActuatorOwnership(Actuator entity, Long groupsId) {
        boolean belongsToGroup = locationsRepository
                .findByLocationIdAndGroupGroupId(entity.getLocation().getLocationId(), groupsId)
                .isPresent();
        if (!belongsToGroup) {
            throw new ActuatorNotFoundException(entity.getActuatorId());
        }
    }

    @Override
    @Transactional
    public Long createActuator(Long groupsId, ActuatorRequest request) {

        // 조회 및 검증 - 사용자는 locationId를 모르므로 그룹 내 이름으로 찾음
        Location locations = locationsRepository.findByGroupGroupIdAndLocationName(groupsId, request.locationName())
                .orElseThrow(() -> LocationNotFoundException.notFoundLocationByName(request.locationName()));

        // 등록된 액추에이터 종류인지 확인
        if (!actuatorTypeRepository.existsById(request.actuatorType())) {
            throw new ActuatorTypeNotFoundException(request.actuatorType());
        }

        Actuator entity = Actuator.builder()
                .location(locations)
                .sensorName(request.sensorName())
                .actuatorType(request.actuatorType())
                .currentState(request.currentState())
                .stateUpdatedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
        return actuatorRepository.save(entity).getActuatorId();
    }

    @Override
    public ActuatorResponse getActuatorById(Long groupId, Long actuatorId) {
        Actuator entity = actuatorRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException(actuatorId));

        validateActuatorOwnership(entity, groupId);

        return ActuatorResponse.from(entity);
    }

    @Override
    public List<ActuatorResponse> getActuatorsByLocationId(Long groupId, Long locationId) {

        // 위치가 요청 그룹 소속인지 교차 검증
        locationsRepository.findByLocationIdAndGroupGroupId(locationId, groupId)
                .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(locationId));

        return actuatorRepository.findByLocationLocationId(locationId).stream()
                .map(ActuatorResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void updateActuatorState(Long groupsId, Long actuatorId, Map<String, Object> newState, ExecutedByType executedByType, Long actingUserId) {

        boolean isUserRequest = executedByType == ExecutedByType.USER;

        // 서비스 계층 방어 코드: 상태값이 비어있다면 400 Bad Request 예외 발생
        if (newState == null || newState.isEmpty()) {
            throw new InvalidActuatorValueException("액추에이터 제어 상태 값(newState)은 비어있음");
        }

        Actuator entity = actuatorRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException(actuatorId));

        // 사용자 요청일때만 소유권 체크 (시스템 요청은 groupId 필요가없을수있음)
        if (isUserRequest) {
            validateActuatorOwnership(entity, groupsId);
        }

        entity.updateState(newState);

        // 실행 이력 기록 (누가 어떤걸 어떻게 바꿨는지)
        actuatorRunLogService.recordRunLogs(entity, newState, executedByType, isUserRequest ? actingUserId : null);
    }


    @Override
    @Transactional
    public void updateActuatorName(Long groupsId, Long actuatorId, String newName) {

        if (newName == null || newName.isBlank()) {
            throw new InvalidActuatorValueException("액추에이터 이름은 비어있을 수 없습니다.");
        }

        Actuator entity = actuatorRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException(actuatorId));

        validateActuatorOwnership(entity, groupsId);

        entity.updateName(newName);
    }

    @Override
    @Transactional
    public void deleteActuatorById(Long groupsId, Long actuatorId) {
        Actuator entity = actuatorRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException(actuatorId));
        validateActuatorOwnership(entity, groupsId);

        actuatorRunLogRepository.deleteByActuatorActuatorId(actuatorId); // 자식(실행로그)부터 삭제
        actuatorRepository.delete(entity); // 그 다음 부모(액추에이터) 삭제
    }


    @Override
    @Transactional
    public void deleteAll(Long groupsId) {
        List<Long> locationIds = locationsRepository.findAllByGroupGroupId(groupsId).stream()
                .map(LocationListResponse::locationId)
                .toList();

        actuatorRunLogRepository.deleteAllByActuatorLocationLocationIdIn(locationIds);
        actuatorRepository.deleteAllByLocationLocationIdIn(locationIds);
    }

    @Override
    @Transactional
    public void deleteAllByLocationId(Long locationId) {
        actuatorRunLogRepository.deleteAllByActuatorLocationLocationId(locationId);
        actuatorRepository.deleteAllByLocationLocationId(locationId);
    }
}
