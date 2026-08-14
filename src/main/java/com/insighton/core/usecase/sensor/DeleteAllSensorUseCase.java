package com.insighton.core.usecase.sensor;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.sensors.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteAllSensorUseCase {
    private final GroupMemberService groupMemberService;
    private final SensorService sensorService;

    @Transactional
    public void deleteAll(Long userId, Long groupId) {
        groupMemberService.validateGroupAdmin(groupId, userId);
        sensorService.deleteAll(groupId);
    }
}
