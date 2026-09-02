package com.insighton.core.domain.actuators.service;

import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuators.dto.ActuatorRequest;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;

import java.util.List;
import java.util.Map;

// 액추에이터 순수 영속성 서비스 - 권한 검증은 usecase.actuator 패키지의 UseCase들로 이동
public interface ActuatorService {
    // 새로운 액추에이터 생성
    Long createActuator(Long groupsId, ActuatorRequest request);

    // 단일 액추에이터 상세 조회
    ActuatorResponse getActuatorById(Long groupId, Long actuatorId);

    // 위치 ID별 액추에이터 목록 조회
    List<ActuatorResponse> getActuatorsByLocationId(Long groupId, Long locationId);

    // 그룹 소속 액추에이터 전체 조회 (공급자 장치 매핑 현황 계산용)
    List<ActuatorResponse> getActuatorsByGroupId(Long groupId);

    // actingUserId: 권한 검증용이 아니라 실행 이력(run log) 기록 대상 - 시스템 호출은 null
    void updateActuatorState(Long groupsId, Long actuatorId, Map<String, Object> newState, ExecutedByType executedByType, Long actingUserId);

    // 액추에이터 장비 이름 수정 (권한 체크 필요)
    void updateActuatorName(Long groupsId, Long actuatorId, String newName);

    // 단일 액추에이터 삭제 (권한 체크 필요)
    void deleteActuatorById(Long groupsId, Long actuatorId);

    // 전체 액추에이터 삭제 (권한 체크 필요)
    void deleteAll(Long groupsId);

    // 장소 삭제 캐스케이드용 - 권한 체크는 호출부(LocationDeleteUseCase)에서 이미 수행됨
    void deleteAllByLocationId(Long locationId);
}
