package com.insighton.core.usecase.actuator;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

// 액추에이터 상태(전원/모드/온도 등) 변경 - 매니저 이상 권한 필요
@UseCase
@RequiredArgsConstructor
public class UpdateActuatorStateUseCase {
    private final GroupMemberService groupMemberService;
    private final ActuatorControlFacade actuatorControlFacade;

    public void updateActuatorState(Long userId, Long groupsId, Long actuatorId, Map<String, Object> newState) {
        // 매니저 이상 권한 검증 - Facade는 소유권만 확인하므로 역할 검증은 여기 유지
        groupMemberService.validateGroupAdmin(groupsId, userId);

        // 조회, 소유권, 상태 병합, 명령 검증, 공급자 Adapter 호출, 성공 후 저장은 전부 Facade가 담당
        // @Transactional 없음 - Facade 안에서 외부 호출 후 저장하므로 트랜잭션으로 감쌀 수 없음
        actuatorControlFacade.control(groupsId, actuatorId, newState, ExecutedByType.USER, userId);
    }
}
