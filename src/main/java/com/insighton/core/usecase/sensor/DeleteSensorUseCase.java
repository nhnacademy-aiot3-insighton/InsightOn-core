package com.insighton.core.usecase.sensor;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.sensors.event.SensorDeletedEvent;
import com.insighton.core.domain.sensors.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteSensorUseCase {
    private final GroupMemberService groupMemberService;
    private final SensorService sensorService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void deleteSensor(Long userId, Long sensorId) {
        Long groupId = sensorService.getSensorGroupId(sensorId);
        groupMemberService.validateGroupAdmin(groupId, userId);
        sensorService.deleteSensor(sensorId);

        // 센서 삭제시 룰엔진에게 보낼 이벤트
        eventPublisher.publishEvent(new SensorDeletedEvent(sensorId));
    }
}
