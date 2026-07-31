package com.insighton.core.actuators.service.impl;

import com.insighton.core.actuators.dto.ActuatorsRequest;
import com.insighton.core.actuators.dto.ActuatorsResponse;
import com.insighton.core.actuators.entity.ActuatorsEntity;
import com.insighton.core.actuators.exception.ActuatorNotFoundException;
import com.insighton.core.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.actuators.repository.ActuatorsRepository;
import com.insighton.core.actuators.service.ActuatorsService;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.location.dto.response.LocationsListResponse;
import com.insighton.core.location.entity.Locations;
import com.insighton.core.location.exception.LocationNotFoundException;
import com.insighton.core.location.repository.LocationsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActuatorsServiceImpl implements ActuatorsService {

    private final ActuatorsRepository actuatorsRepository;
    private final LocationsRepository locationsRepository; // 소유권 교차검증용
    private final GroupMembersService groupMembersService;


    private void validateManagerRole(Long userId, Long groupsId) {
        GroupMembers member = groupMembersService.validateGroupMembers(groupsId, userId);
        if (member.isMember()) {
            throw NoPermissionException.forAdmin(member.getGroupMemberId());
        }
    }

    // 대상 액추에이터가 실제로 groupsId 소속인지 확인 (locationId -> location.groupId 교차검증)
    // LocationsServiceImpl과 동일하게, 존재하지 않는 것처럼 404로 통일 (다른 그룹 리소스 존재 여부를 노출하지 않기 위해)
    private void validateActuatorOwnership(ActuatorsEntity entity, Long groupsId) {

        boolean belongsToGroup = locationsRepository
                .findByLocationIdAndGroups_GroupId(entity.getLocationId().getLocationId(), groupsId)
                .isPresent();
        if (!belongsToGroup) {
            throw new ActuatorNotFoundException("액추에이터를 찾을 수 없습니다. (ID: " + entity.getActuatorId() + ")");
        }
    }

    private void validateGroupMembership(Long userId, Long groupId){
        groupMembersService.validateGroupMembers(groupId,userId);
    }

    @Override
    @Transactional
    public Long createActuator(Long userId, Long groupsId, ActuatorsRequest request) {

        // 쓰기 작업 권한 체크
        validateManagerRole(userId, groupsId);

        // 조회 및 검증
        Locations locations = locationsRepository.findByLocationIdAndGroups_GroupId(request.locationId(), groupsId)
                        .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(request.locationId()));


        // 전달받은 요청 정보로 액추에이터 엔티티 생성 및 저장[cite: 6]
        ActuatorsEntity entity = ActuatorsEntity.builder()
                .locationId(locations)
                .deviceName(request.deviceName())
                .actuatorType(request.actuatorType())
                .currentState(request.currentState())
                .stateUpdatedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
        return actuatorsRepository.save(entity).getActuatorId();
    }

    @Override
    public ActuatorsResponse getActuatorById(Long userId, Long groupId, Long actuatorId) {
        // 단건 조회 수행, 없을 시 404 예외 발생[cite: 6]
        ActuatorsEntity entity = actuatorsRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException("액추에이터를 찾을 수 없습니다. (ID: " + actuatorId + ")"));

        validateGroupMembership(userId, groupId);
        validateActuatorOwnership(entity, groupId);

        return ActuatorsResponse.from(entity);
    }

    @Override
    public List<ActuatorsResponse> getActuatorsByLocationId(Long userId, Long groupId, Long locationId) {

        validateGroupMembership(userId, groupId);

        // 위치 ID를 기준으로 목록 조회 후 DTO 변환[cite: 6]
        return actuatorsRepository.findByLocationId_LocationId(locationId).stream()
                .map(ActuatorsResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void updateActuatorState(Long userId, Long groupsId, Long actuatorId, Map<String, Object> newState, boolean isSystemRequest) {

        // AI나 룰엔진 등 시스템 요청인 경우 권한 검증을 생략함
        if (!isSystemRequest) {
            validateManagerRole(userId, groupsId);
        }

        // 서비스 계층 방어 코드: 상태값이 비어있다면 400 Bad Request 예외 발생[cite: 6]
        if (newState == null || newState.isEmpty()) {
            throw new InvalidActuatorValueException("액추에이터 제어 상태 값(newState)은 비어있을 수 없습니다.");
        }

        // 대상 액추에이터 조회 및 상태 갱신[cite: 6]
        ActuatorsEntity entity = actuatorsRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException("액추에이터를 찾을 수 없습니다. (ID: " + actuatorId + ")"));

        // 사용자 요청일때만 소유권 체크 (시스템 요청은 groupId 필요가없을수있음)
        if(!isSystemRequest){
            validateActuatorOwnership(entity, groupsId);
        }

        entity.updateState(newState);
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
        ActuatorsEntity entity = actuatorsRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException("액추에이터를 찾을 수 없습니다. (ID: " + actuatorId + ")"));

        validateActuatorOwnership(entity, groupsId);

        entity.updateName(newName);
    }

    @Override
    @Transactional
    public void deleteActuatorById(Long userId, Long groupsId, Long actuatorId) {
        validateManagerRole(userId, groupsId);

        ActuatorsEntity entity = actuatorsRepository.findById(actuatorId)
                .orElseThrow(() -> new ActuatorNotFoundException("삭제할 액추에이터를 찾을 수 없습니다. (ID: " + actuatorId + ")"));
        validateActuatorOwnership(entity, groupsId);

        actuatorsRepository.delete(entity);
    }

    @Override
    @Transactional
    public void deleteAll(Long userId, Long groupsId) {
        validateManagerRole(userId, groupsId);

        List<Long> locationIds = locationsRepository.findAllByGroups_GroupId(groupsId).stream()
                .map(LocationsListResponse::locationId)
                .toList();
        actuatorsRepository.deleteAllByLocationId_LocationIdIn(locationIds); // groupsId 소속 location만 스코프
    }
}