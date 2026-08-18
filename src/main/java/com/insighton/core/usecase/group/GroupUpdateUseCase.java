package com.insighton.core.usecase.group;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.dto.request.GroupUpdateRequest;
import com.insighton.core.domain.groups.event.GroupRegionUpdateEvent;
import com.insighton.core.domain.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.domain.groups.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GroupUpdateUseCase {
    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 그룹 수정
     *
     * @param request Group 수정 요청 정보
     * @param userId  login한 user의 ID
     * @param groupId 수정하려는 group의 ID
     */
    @Transactional
    public void updateGroup(GroupUpdateRequest request, Long userId, Long groupId) {
        if (groupMemberService.isGroupAdmin(groupId, userId)) {
            groupService.updateGroup(request, groupId);

            if (request.groupRegion() != null) {
                eventPublisher.publishEvent(new GroupRegionUpdateEvent(groupId, request.groupRegion()));
            }
            return;
        }
        throw new UnAuthorizedAccessException(userId);
    }
}
