package com.insighton.core.usecase.group;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GroupTokenUseCase {
    private final GroupMemberService groupMemberService;
    private final GroupService groupService;

    /**
     * 토큰 재발급
     *
     * @param groupId 재발급 하려는 group의 ID
     * @param userId  재발급 하려는 user의 ID
     */
    @Transactional
    public void newInviteToken(Long userId, Long groupId) {
        groupMemberService.validateGroupAdmin(groupId, userId);

        groupService.newInviteToken(groupId);
    }
}
