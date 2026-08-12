package com.insighton.core.usecase.actuator;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

// 그룹 소속 액추에이터 전체 삭제 - 매니저 이상 권한 필요
@UseCase
@RequiredArgsConstructor
public class DeleteAllActuatorUseCase {
    private final GroupMemberService groupMemberService;
    private final ActuatorService actuatorService;

    @Transactional
    public void deleteAll(Long userId, Long groupsId) {
        groupMemberService.validateGroupAdmin(groupsId, userId);
        actuatorService.deleteAll(groupsId);
    }
}
