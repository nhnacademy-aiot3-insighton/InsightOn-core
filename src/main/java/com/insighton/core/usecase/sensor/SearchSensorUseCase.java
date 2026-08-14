package com.insighton.core.usecase.sensor;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.dto.SensorUpdateRequest;
import com.insighton.core.domain.sensors.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class SearchSensorUseCase {
    private final GroupMemberService groupMemberService;
    private final SensorService sensorService;

    @Transactional(readOnly = true)
    public List<SensorResponse> searchSensors(Long userId, Long groupId, Long id, String eui,
                                              SensorUpdateRequest request) {
        groupMemberService.validateGroupMembers(groupId, userId);
        return sensorService.searchSensors(groupId, id, eui, request);
    }
}
