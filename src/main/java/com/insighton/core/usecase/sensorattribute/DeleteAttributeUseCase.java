package com.insighton.core.usecase.sensorattribute;


import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.sensorattributes.service.SensorAttributeService;
import com.insighton.core.domain.sensors.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteAttributeUseCase {
    private final GroupMemberService groupMemberService;
    private final SensorAttributeService sensorAttributeService;
    private final SensorService sensorService;

    @Transactional
    public void deleteAttribute(Long userId, Long sensorId, String metricKey){
        Long groupId = sensorService.getSensorGroupId(sensorId);
        groupMemberService.validateGroupAdmin(groupId, userId);
        sensorAttributeService.deleteAttribute(sensorId, metricKey);
    }
}
