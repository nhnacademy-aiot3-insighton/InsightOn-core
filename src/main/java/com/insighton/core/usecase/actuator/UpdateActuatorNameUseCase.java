package com.insighton.core.usecase.actuator;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

// 액추에이터 이름 수정 - 매니저 이상 권한 필요
@UseCase
@RequiredArgsConstructor
public class UpdateActuatorNameUseCase {
    private final GroupMemberService groupMemberService;
    private final ActuatorService actuatorService;

    @Transactional
    public void updateActuatorName(Long userId, Long groupsId, Long actuatorId, String newName) {
        groupMemberService.validateGroupAdmin(groupsId, userId);
        actuatorService.updateActuatorName(groupsId, actuatorId, newName);
    }
}
