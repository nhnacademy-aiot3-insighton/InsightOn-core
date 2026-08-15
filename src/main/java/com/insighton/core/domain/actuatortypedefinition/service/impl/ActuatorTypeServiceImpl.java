package com.insighton.core.domain.actuatortypedefinition.service.impl;

import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import com.insighton.core.domain.actuatortypedefinition.dto.ActuatorTypeCreateRequest;
import com.insighton.core.domain.actuatortypedefinition.dto.ActuatorTypeResponse;
import com.insighton.core.domain.actuatortypedefinition.entity.ActuatorTypeDefinition;
import com.insighton.core.domain.actuatortypedefinition.exception.ActuatorTypeAlreadyExistsException;
import com.insighton.core.domain.actuatortypedefinition.exception.ActuatorTypeInUseException;
import com.insighton.core.domain.actuatortypedefinition.exception.ActuatorTypeNotFoundException;
import com.insighton.core.domain.actuatortypedefinition.repository.ActuatorTypeRepository;
import com.insighton.core.domain.actuatortypedefinition.service.ActuatorTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// ActuatorTypeService의 JPA 구현체
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActuatorTypeServiceImpl implements ActuatorTypeService {

    private final ActuatorRepository actuatorRepository;
    private final ActuatorTypeRepository actuatorTypeRepository;

    @Override
    public List<ActuatorTypeResponse> getAllActuatorTypes() {
        return actuatorTypeRepository.findAll().stream()
                .map(ActuatorTypeResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void createActuatorType(ActuatorTypeCreateRequest request) {
        // 이미 존재하는 타입 코드면 생성 거부
        if(actuatorTypeRepository.existsByTypeCodeIgnoreCase(request.typeCode())){
            throw new ActuatorTypeAlreadyExistsException(request.typeCode());
        }

        ActuatorTypeDefinition entity = ActuatorTypeDefinition.builder()
                .typeCode(request.typeCode())
                .typeName(request.typeName())
                .description(request.description())
                .build();

        actuatorTypeRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteActuatorType(String typeCode) {
        if (!actuatorTypeRepository.existsById(typeCode)) {
            throw new ActuatorTypeNotFoundException(typeCode);
        }

        // 이 종류를 쓰고 있는 액추에이터가 있으면 삭제 금지 - 그룹 삭제 때 액추에이터 빼먹었던 것과 같은 실수 방지
        long usageCount = actuatorRepository.countByActuatorType();
        if (usageCount > 0) {
            throw new ActuatorTypeInUseException(typeCode, usageCount);
        }

        actuatorTypeRepository.deleteById(typeCode);
    }
}
