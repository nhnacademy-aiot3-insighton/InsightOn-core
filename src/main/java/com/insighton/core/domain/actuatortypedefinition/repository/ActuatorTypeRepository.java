package com.insighton.core.domain.actuatortypedefinition.repository;

import com.insighton.core.domain.actuatortypedefinition.entity.ActuatorTypeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActuatorTypeRepository extends JpaRepository<ActuatorTypeDefinition, String> {

    // 대소문자 무시하고 이미 존재하는 타입코드인지 확인 - 중복 등록 방지용
    boolean existsByTypeCodeIgnoreCase(String typeCode);

}
