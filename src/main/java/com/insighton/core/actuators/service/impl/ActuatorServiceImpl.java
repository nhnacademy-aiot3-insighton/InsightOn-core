package com.insighton.core.actuators.service.impl;

import com.insighton.core.actuator_run_logs.entity.ExecutedByType;
import com.insighton.core.actuator_run_logs.repository.ActuatorRunLogRepository;
import com.insighton.core.actuator_run_logs.service.ActuatorRunLogService;
import com.insighton.core.actuators.dto.ActuatorRequest;
import com.insighton.core.actuators.dto.ActuatorResponse;
import com.insighton.core.actuators.entity.Actuator;
import com.insighton.core.actuators.exception.ActuatorNotFoundException;
import com.insighton.core.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.actuators.repository.ActuatorRepository;
import com.insighton.core.actuators.service.ActuatorService;
import com.insighton.core.groupmember.entity.GroupMember;
import com.insighton.core.groupmember.service.GroupMemberService;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.location.dto.response.LocationListResponse;
import com.insighton.core.location.entity.Location;
import com.insighton.core.location.exception.LocationNotFoundException;
import com.insighton.core.location.repository.LocationRepository;
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
    private final GroupMemberService groupMembersService; // 그룹 멤버 권한 검증용 서비스
    private final ActuatorRunLogService actuatorRunLogService; // 제어 이력 기록용
    private final ActuatorRunLogRepository actuatorRunLogRepository;


    // 요청자가 해당 그룹의 MANAGER 이상 권한을 가졌는지 검증
    private void validateManagerRole(Long userId, Long groupsId) {
        GroupMember member = groupMembersService.validateGroupMembers(groupsId, userId);
        if (member.isMember()) {
            throw NoPermissionException.forAdmin(member.getGroupMemberId());
        }
    }

    // 대상 액추에이터가 실제로 groupsId 소속인지 확인 (locationId -> location.groupId 교차검증)
    // LocationsServiceImpl과 동일하게, 존재하지 않는 것처럼 404로 통일 (다른 그룹 리소스 존재 여부를 노출하지 않기 위해)
    private void validateActuatorOwnership(Actuator entity, Long groupsId) {

        boolean belongsToGroup = locationsRepository
                .findByLocationIdAndGroupGroupId(entity.getLocation().getLocationId(), groupsId)
                .isPresent();
        if (!belongsToGroup) {
            throw new ActuatorNotFoundException(entity.getActuatorId());
        }
    }

    private void validateGroupMembership(Long userId, Long groupId) {
        groupMembersService.validateGroupMembers(groupId, userId);
    }

    @Override
    @Transactional
    public Long createActuator(Long userId, Long groupsId, ActuatorRequest request) {

        // 쓰기 작업 권한 체크
        validateManagerRole(userId, groupsId);

        // 조회 및 검증
        Location locations = locationsRepository.findByLocationIdAndGroupGroupId(request.locationId(), groupsId)
                .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(request.locationId()));


        // 전달받은 요청 정보로 액추에이터 엔티티 생성 및 저장[cite: 6]
        Actuator entity = Actuator.builder()
                .location(locations)
                .deviceName(request.deviceName())
                .actuatorType(request.actuatorType())
                .currentState(request.currentState())
                .stateUpdatedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
        return actuatorRepository.save(entity).getActuatorId();
    }

    @Override
    public ActuatorResponse getActuatorById(Long userId, Long groupId, Long actuatorId) {
        // 단건 조회 수행, 없을 시 404 예외 발생[cite: 6]
        Actuator entity = actuatorRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException(actuatorId));

        validateGroupMembership(userId, groupId);
        validateActuatorOwnership(entity, groupId);

        return ActuatorResponse.from(entity);
    }

    @Override
    public List<ActuatorResponse> getActuatorsByLocationId(Long userId, Long groupId, Long locationId) {

        validateGroupMembership(userId, groupId);

        // 위치가 요청 그룹 소속인지 교차 검증
        locationsRepository.findByLocationIdAndGroupGroupId(locationId, groupId)
                .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(locationId));

        // 위치 ID를 기준으로 목록 조회 후 DTO 변환[cite: 6]
        return actuatorRepository.findByLocationLocationId(locationId).stream()
                .map(ActuatorResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void updateActuatorState(Long userId, Long groupsId, Long actuatorId, Map<String, Object> newState, ExecutedByType executedByType) {

        boolean isUserRequest = executedByType == ExecutedByType.USER;

        // AI나 룰엔진 등 시스템 요청인 경우 권한 검증을 생략함
        if (isUserRequest) {
            validateManagerRole(userId, groupsId);
        }

        // 서비스 계층 방어 코드: 상태값이 비어있다면 400 Bad Request 예외 발생
        if (newState == null || newState.isEmpty()) {
            throw new InvalidActuatorValueException("액추에이터 제어 상태 값(newState)은 비어있음");
        }

        // 대상 액추에이터 조회 및 상태 갱신
        Actuator entity = actuatorRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException(actuatorId));

        // 사용자 요청일때만 소유권 체크 (시스템 요청은 groupId 필요가없을수있음)
        if (isUserRequest) {
            validateActuatorOwnership(entity, groupsId);
        }

        entity.updateState(newState);

        // 실행 이력 기록 (누가 어떤걸 어떻게 바꿨는지)
        actuatorRunLogService.recordRunLogs(entity, newState, executedByType, isUserRequest ? userId : null);


    }


    @Override
    @Transactional
    public void updateActuatorName(Long userId, Long groupsId, Long actuatorId, String newName) {
        validateManagerRole(userId, groupsId); // 쓰기 작업 권한 체크

        // 이름 유효성 검증[cite: 6]
        if (newName == null || newName.isBlank()) {
            throw new InvalidActuatorValueException("액추에이터 이름은 비어있을 수 없습니다.");
        }

        // 대상 액추에이터 조회 및 이름 수정[cite: 6]
        Actuator entity = actuatorRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException(actuatorId));

        validateActuatorOwnership(entity, groupsId);

        entity.updateName(newName);
    }

    @Override
    @Transactional
    public void deleteActuatorById(Long userId, Long groupsId, Long actuatorId) {
        validateManagerRole(userId, groupsId);

        Actuator entity = actuatorRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException(actuatorId));
        validateActuatorOwnership(entity, groupsId);

        actuatorRunLogRepository.deleteByActuatorActuatorId(actuatorId); // 자식(실행로그)부터 삭제
        actuatorRepository.delete(entity); // 그 다음 부모(액추에이터) 삭제
    }


    @Override
    @Transactional
    public void deleteAll(Long userId, Long groupsId) {
        validateManagerRole(userId, groupsId);

        List<Long> locationIds = locationsRepository.findAllByGroupGroupId(groupsId).stream()
                .map(LocationListResponse::locationId)
                .toList();

        // 이미 만들어뒀던 위치 범위 삭제 메서드 재사용 (그룹→장소 캐스케이드 삭제 때 쓰던 것과 동일)
        actuatorRunLogRepository.deleteAllByActuatorLocationLocationIdIn(locationIds);
        actuatorRepository.deleteAllByLocationLocationIdIn(locationIds); // groupsId 소속 location만 스코프
    }
}