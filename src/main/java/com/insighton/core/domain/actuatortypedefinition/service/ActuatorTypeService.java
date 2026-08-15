package com.insighton.core.domain.actuatortypedefinition.service;

import com.insighton.core.domain.actuatortypedefinition.dto.ActuatorTypeCreateRequest;
import com.insighton.core.domain.actuatortypedefinition.dto.ActuatorTypeResponse;

import java.util.List;

// 액추에이터 종류 마스터데이터 관리 - 그룹 범위가 아닌 전역 카탈로그라 권한 검증 없이 내부(internal) API로만 노출
public interface ActuatorTypeService {
    // 액추에이터 종류 찾기
    List<ActuatorTypeResponse> getAllActuatorTypes();

    // 액추에이터 종류 생성
    void createActuatorType(ActuatorTypeCreateRequest request);


    // 액우에이터 종류 삭제
    void deleteActuatorType(String typeCode);

}
