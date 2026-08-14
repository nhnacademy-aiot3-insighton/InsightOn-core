package com.insighton.core.usecase.actuator;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 위치별 액추에이터 목록 조회 - 그룹 멤버면 조회 가능
@UseCase
@RequiredArgsConstructor
public class GetActuatorsByLocationUseCase {
    private final GroupMemberService groupMemberService;
    private final ActuatorService actuatorService;

    @Transactional(readOnly = true)
    public List<ActuatorResponse> getActuatorsByLocationId(Long userId, Long groupId, Long locationId) {
        groupMemberService.validateGroupMembers(groupId, userId);
        return actuatorService.getActuatorsByLocationId(groupId, locationId);
    }
}
