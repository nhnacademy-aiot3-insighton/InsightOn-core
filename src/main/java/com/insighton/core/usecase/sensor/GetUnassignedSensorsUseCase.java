package com.insighton.core.usecase.sensor;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 장소 미배정 센서 목록 조회 - 그룹 멤버면 조회 가능
@UseCase
@RequiredArgsConstructor
public class GetUnassignedSensorsUseCase {
    private final GroupMemberService groupMemberService;
    private final SensorService sensorService;

    @Transactional(readOnly = true)
    public List<SensorResponse> getUnassignedSensors(Long userId, Long groupId) {
        groupMemberService.validateGroupMembers(groupId, userId);
        return sensorService.getUnassignedSensors(groupId);
    }
}
