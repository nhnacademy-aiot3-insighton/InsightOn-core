package com.insighton.core.actuators.service;

import com.insighton.core.actuators.dto.ActuatorsRequest;
import com.insighton.core.actuators.dto.ActuatorsResponse;

import java.util.List;
import java.util.Map;

// 액추에이터 비지니스 로직 서빙 인터페이스
public interface ActuatorsService {
    // 새로운 액추에이터 생성
    Long createActuator(ActuatorsRequest request);

    // 단일 액추에이터 상세 조회
    ActuatorsResponse getActuatorById(Long actuatorId);

    // 위치 ID별 액추에이터 목록 조회
    List<ActuatorsResponse> getActuatorsByLocationId(Long locationId);

    // 액추에이터 상태값(JSONB) 제어 및 갱신
    void updateActuatorState(Long actuatorId, Map<String, Object> newState);

    // 액추에이터 장비 이름 수정
    void updateActuatorName(Long actuatorId, String newName);

    // 단일 액추에이터 삭제
    void deleteActuatorById(Long actuatorId);

    // 전데 액추에이터 삭제
    void deleteAll();
}
