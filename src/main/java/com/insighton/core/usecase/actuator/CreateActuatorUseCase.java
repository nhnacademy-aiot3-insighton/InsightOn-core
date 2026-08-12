package com.insighton.core.usecase.actuator;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuators.dto.ActuatorRequest;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

// 액추에이터 생성 - 매니저 이상 권한 필요
@UseCase
@RequiredArgsConstructor
public class CreateActuatorUseCase {
    private final GroupMemberService groupMemberService;
    private final ActuatorService actuatorService;

    @Transactional
    public Long createActuator(Long userId, Long groupsId, ActuatorRequest request) {
        groupMemberService.validateGroupAdmin(groupsId, userId);
        return actuatorService.createActuator(groupsId, request);
    }
}
