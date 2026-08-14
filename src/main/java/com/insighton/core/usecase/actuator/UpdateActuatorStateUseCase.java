package com.insighton.core.usecase.actuator;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import com.insighton.core.domain.actuators.policy.ActuatorCommandPreset;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// 액추에이터 상태(전원/모드/온도 등) 변경 - 매니저 이상 권한 필요
@UseCase
@RequiredArgsConstructor
public class UpdateActuatorStateUseCase {
    private final GroupMemberService groupMemberService;
    private final ActuatorService actuatorService;

    @Transactional
    public void updateActuatorState(Long userId, Long groupsId, Long actuatorId, Map<String, Object> newState) {
        groupMemberService.validateGroupAdmin(groupsId, userId);

        // 명령 값 검증을 위해 actuatorType이 필요 - 조회하면서 소유권 검증도 같이 됨
        ActuatorResponse actuator = actuatorService.getActuatorById(groupsId, actuatorId);
        ActuatorCommandPreset.validateCommandValues(actuator.actuatorType(), newState);

        // 컨트롤러를 통한 사용자 요청이므로 항상 USER + 호출자 userId를 실행이력에 남김
        actuatorService.updateActuatorState(groupsId, actuatorId, newState, ExecutedByType.USER, userId);
    }
}
