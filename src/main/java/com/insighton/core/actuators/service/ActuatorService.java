package com.insighton.core.actuators.service;

import com.insighton.core.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.actuators.dto.ActuatorRequest;
import com.insighton.core.actuators.dto.ActuatorResponse;

import java.util.List;
import java.util.Map;

// 액추에이터 비즈니스 로직 서빙 인터페이스
public interface ActuatorService {
    // 새로운 액추에이터 생성 (권한 체크 필요)
    Long createActuator(Long userId, Long groupsId, ActuatorRequest request);

    // 단일 액추에이터 상세 조회 (조회는 권한 체크 유지 혹은 생략)
    ActuatorResponse getActuatorById(Long userId, Long groupId, Long actuatorId);

    // 위치 ID별 액추에이터 목록 조회
    List<ActuatorResponse> getActuatorsByLocationId(Long userId, Long groupId, Long locationId);

    // 액추에이터 상태값(JSONB) 제어 및 갱신 (시스템/사용자 분기)
    // ActuatorsService.java - 시그니처 변경
    void updateActuatorState(Long userId, Long groupsId, Long actuatorId, Map<String, Object> newState, ExecutedByType executedByType);

    // 액추에이터 장비 이름 수정 (권한 체크 필요)
    void updateActuatorName(Long userId, Long groupsId, Long actuatorId, String newName);

    // 단일 액추에이터 삭제 (권한 체크 필요)
    void deleteActuatorById(Long userId, Long groupsId, Long actuatorId);

    // 전체 액추에이터 삭제 (권한 체크 필요)
    void deleteAll(Long userId, Long groupsId);
}