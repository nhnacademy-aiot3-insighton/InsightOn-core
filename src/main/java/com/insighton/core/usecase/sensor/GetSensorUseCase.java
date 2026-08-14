package com.insighton.core.usecase.sensor;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetSensorUseCase {
    private final GroupMemberService groupMemberService;
    private final SensorService sensorService;

    @Transactional(readOnly = true)
    public SensorResponse getSensorById(Long userId, Long sensorId) {
        Long groupId = sensorService.getSensorGroupId(sensorId);
        groupMemberService.validateGroupMembers(groupId, userId);
        return sensorService.getSensorById(sensorId);
    }
}
