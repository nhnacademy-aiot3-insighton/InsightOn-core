package com.insighton.core.actuators.service.impl;

import com.insighton.core.actuators.dto.ActuatorsRequest;
import com.insighton.core.actuators.dto.ActuatorsResponse;
import com.insighton.core.actuators.entity.ActuatorsEntity;
import com.insighton.core.actuators.repository.ActuatorsRepository;
import com.insighton.core.actuators.service.ActuatorsService;
import com.insighton.core.exception.CustomException;
import com.insighton.core.exception.ErrorCode;
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

    @Override
    @Transactional
    public Long createActuator(ActuatorsRequest request) {

        // 새로운 액추에이서 생성
        ActuatorsEntity entity = ActuatorsEntity.builder()
                .locationId(request.locationId())
                .deviceName(request.deviceName())
                .actuatorType(request.actuatorType())
                .currentState(request.currentState())
                .stateUpdatedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
        return actuatorsRepository.save(entity).getActuatorId();
    }

    @Override
    public ActuatorsResponse getActuatorById(Long actuatorId) {

        // 액추에이터 아이디로 데이터 조회
        ActuatorsEntity entity = actuatorsRepository.findById(actuatorId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTUATOR_NOT_FOUND));

        return ActuatorsResponse.from(entity);
    }

    @Override
    public List<ActuatorsResponse> getActuatorsByLocationId(Long locationId) {

        // 위치 ID로 액추에이터 데이터 조회
        List<ActuatorsResponse> list= actuatorsRepository.findByLocationId(locationId).stream()
                .map(ActuatorsResponse::from)
                .toList();
        return list;
    }

    @Override
    @Transactional
    public void updateActuatorState(Long actuatorId, Map<String, Object> newState) {
        if(newState == null || newState.isEmpty()){
            throw new CustomException(ErrorCode.NO_NEW_ACTUATOR_STATE);
        }

        ActuatorsEntity entity = actuatorsRepository.findById(actuatorId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTUATOR_NOT_FOUND));

        entity.updateState(newState);

    }

    @Override
    @Transactional
    public void updateActuatorName(Long actuatorId, String newName) {
        // 전달 받은 상태 Map 객체의 Null/Empty 유효성 검증
        if (newName == null || newName.isEmpty()){
            throw new CustomException(ErrorCode.NO_NEW_ACTUATOR_NAME);
        }

        ActuatorsEntity entity = actuatorsRepository.findById(actuatorId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTUATOR_NOT_FOUND));

        entity.updateName(newName);

    }

    @Override
    @Transactional
    public void deleteActuatorById(Long actuatorId) {
        ActuatorsEntity entity = actuatorsRepository.findById(actuatorId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTUATOR_NOT_FOUND));

        actuatorsRepository.delete(entity);

    }

    @Override
    @Transactional
    public void deleteAll() {
        actuatorsRepository.deleteAll();
    }
}
