package com.insighton.core.usecase.actuator;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogResponse;
import com.insighton.core.domain.actuatorrunlogs.service.ActuatorRunLogService;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

// 액추에이터 실행 이력 페이징 조회 - 그룹 멤버면 조회 가능
@UseCase
@RequiredArgsConstructor
public class GetActuatorRunLogsUseCase {
    private final GroupMemberService groupMemberService;
    private final ActuatorService actuatorService;
    private final ActuatorRunLogService actuatorRunLogService;

    // getActuatorById와 동일한 소유권/권한 검증을 재사용
    @Transactional(readOnly = true)
    public Page<ActuatorRunLogResponse> getActuatorRunLogs(Long userId, Long groupsId, Long actuatorId, Pageable pageable) {
        groupMemberService.validateGroupMembers(groupsId, userId);
        actuatorService.getActuatorById(groupsId, actuatorId); // 존재/소유권 검증 겸용
        return actuatorRunLogService.getRunLogsByActuatorId(actuatorId, pageable);
    }
}
