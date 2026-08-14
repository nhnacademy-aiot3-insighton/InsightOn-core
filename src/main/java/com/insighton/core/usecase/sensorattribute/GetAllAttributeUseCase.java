package com.insighton.core.usecase.sensorattribute;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.sensorattributes.dto.SensorAttributeResponse;
import com.insighton.core.domain.sensorattributes.service.SensorAttributeService;
import com.insighton.core.domain.sensors.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class GetAllAttributeUseCase {
    private final GroupMemberService groupMemberService;
    private final SensorAttributeService sensorAttributeService;
    private final SensorService sensorService;

    @Transactional(readOnly = true)
    public List<SensorAttributeResponse> getAllAttributeBySensorId(Long userId, Long sensorId){
        Long groupId = sensorService.getSensorGroupId(sensorId);
        groupMemberService.validateGroupMembers(groupId, userId);
        return sensorAttributeService.getAllAttributeBySensorId(sensorId);
    }
}
