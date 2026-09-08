package com.insighton.core.usecase.sensor;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.sensors.dto.SensorUpdateRequest;
import com.insighton.core.domain.sensors.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateSensorUseCase {
    private final GroupMemberService groupMemberService;
    private final SensorService sensorService;

    @Transactional
    public void updateSensor(Long userId, Long sensorId, SensorUpdateRequest request) {
        Long groupId = sensorService.getSensorGroupId(sensorId);
        groupMemberService.validateGroupAdmin(groupId, userId);
        sensorService.updateSensor(sensorId, request);
    }
}
