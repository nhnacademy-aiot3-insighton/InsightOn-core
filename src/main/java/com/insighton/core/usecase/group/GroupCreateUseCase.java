package com.insighton.core.usecase.group;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.domain.groups.dto.request.GroupRequest;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GroupCreateUseCase {
    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final GroupRegistrationService groupRegistrationService;


    // ====================== Group Controller ======================

    /**
     * 그룹 가입
     *
     * @param request 그룹 가입 요청 DTO
     */
    @Transactional
    public void joinGroupByToken(GroupMemberJoinRequest request) {
        // inviteToken으로 대상 그룹이 존재하는지 확인 및 조회
        Group group = groupService.validateGroupByInviteToken(request.inviteToken());

        // 그룹 신청서가 대기중 상태인 것이 존재하면 가입 불가능 예외(AlreadyRequestedException)
        groupRegistrationService.validateNoPendingRequest(request.userId());

        groupMemberService.joinGroupByToken(group, request);
    }

    /**
     * 그룹 생성 시 SUPER_MANAGER로 등록되는 (아무나 생성할 수 없도록 제약을 걸어야 함 - 신청서 작성 받기 (group 이름, 사업자 등록 번호 등등))
     *
     * @param groupsCreateRequest 그룹 생성 요청 정보
     * @param userId              그룹을 생성하는 user의 ID
     */
    @Transactional
    public Group createGroup(GroupRequest groupsCreateRequest, Long userId) {
        Group group = groupService.createGroup(groupsCreateRequest);

        groupMemberService.createGroupMember(group, userId);
        return group;
    }
}
