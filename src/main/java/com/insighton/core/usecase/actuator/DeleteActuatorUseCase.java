package com.insighton.core.usecase.actuator;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuators.event.ActuatorDeletedEvent;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

// 액추에이터 단건 삭제 - 매니저 이상 권한 필요
@UseCase
@RequiredArgsConstructor
public class DeleteActuatorUseCase {
    private final GroupMemberService groupMemberService;
    private final ActuatorService actuatorService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void deleteActuatorById(Long userId, Long groupsId, Long actuatorId) {
        groupMemberService.validateGroupAdmin(groupsId, userId);
        actuatorService.deleteActuatorById(groupsId, actuatorId);

        // 액추에이터삭제시 룰엔진에게 보낼 이벤트
        eventPublisher.publishEvent(new ActuatorDeletedEvent(actuatorId));
    }
}
