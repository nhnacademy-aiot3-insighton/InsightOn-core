package com.insighton.core.usecase.groupmember;

import com.insighton.core.adapter.client.internal.AuthClient;
import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.dto.response.AuthUserResponse;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.repository.GroupMemberRepository;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class GroupMemberInviteUserCase {
    private final GroupMemberService groupMemberService;
    private final GroupMemberRepository repository;
    private final AuthClient authClient;
    private final GroupService groupService;

    public void inviteMemberByEmail(Long inviterUserId, Long groupId, String targetEmail) {
        groupMemberService.validateGroupAdmin(groupId, inviterUserId);

        AuthUserResponse user = authClient.getUserResponseEmail(targetEmail);

        if (user == null) {
            throw new IllegalArgumentException("가입되지 않은 이메일 주소 입니다.");
        }

        Long inviteUserId = user.userId();

        groupMemberService.validateUserNotInAnyGroup(inviteUserId);

        Group group = groupService.groupFindById(groupId);

        GroupMember newMember = GroupMember.builder()
                .group(group)
                .userId(inviteUserId)
                .groupRole(GroupMember.GroupRole.MEMBER)
                .build();

        repository.save(newMember);
        log.info("초대가 완료 되었습니다! user name : {}", user.userName());
    }
}
