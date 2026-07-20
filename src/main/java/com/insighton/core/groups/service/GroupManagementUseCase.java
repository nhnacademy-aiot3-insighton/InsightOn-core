package com.insighton.core.service;

import com.insighton.core.groupmember.dto.request.GroupMembersJoinRequest;
import com.insighton.core.groups.dto.response.GroupsListResponse;
import com.insighton.core.groups.dto.response.GroupsResponse;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.service.GroupsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupManagementUseCase {
    private final GroupsService groupService;
    private final GroupMembersService groupMembersService;


    /**
     * 그룹 가입
     * @param request 그룹 가입 요청 DTO
     */
    @Transactional
    public void joinGroupByToken(GroupMembersJoinRequest request) {
        // inviteToken으로 대상 그룹이 존재하는지 확인 및 조회
        groupService.validateGroupByInviteToken(request.inviteToken());

        groupMembersService.joinGroupByToken(request);
    }

    /**
     * 일반 사용자의 초대한 그룹 조회용 (초대장 느낌)
     * 이 초대 토큰은 A 회사의 초대 토큰 입니다 하고 회사 정보를 띄우기.(토큰은 null 값으로 들어감)
     * @param inviteToken 초대 코드
     * @param userId 로그인한 user의 ID
     * @param groupId 내가 지금 보고 있는 group의 ID
     * @return 토큰 값 빼고 group 정보가 들어감
     * return 하기 전에 validateUserExists로 검증
     */
    @Transactional(readOnly = true)
    public GroupsResponse getGroupPreview(String inviteToken, Long userId, Long groupId) {
        // 유저가 존재하는지 검증
        groupMembersService.validateUserExists(userId);

        return groupService.getGroupPreview(inviteToken, userId, groupId);
    }

    /**
     * 현재 로그인한 사용자의 소속 그룹 정보 조회
     * (한 계정당 하나의 그룹만 가입 가능하므로, 로그인 정보 기반으로 해당 그룹 정보를 반환)
     * @param userId login한 user의 ID
     * @return token 정보를 제외한 group의 정보
     */
    @Transactional(readOnly = true)
    public GroupsResponse getMyGroup(Long userId, Long groupId) {
        // 이건 group안에 user가 속해있는지 같이 보기 위한 메서드임...
        GroupMembers members = groupMembersService.validateGroupMembers(groupId, userId);

        // 관리자나 그룹 생성자일 때는
        if (members.getGroupRole() == GroupMembers.GroupRole.MANAGER || members.getGroupRole() == GroupMembers.GroupRole.OWNER) {
            throw NoPermissionException.forResource(members.getGroupMemberId());
        }

        // 대상 그룹 조회 (없을 시 exception 던지기)
        return GroupsResponse.ofPublic(members.getGroups());
    }

    /**
     * 관리자용 group 정보 조회
     * @param userId login한 user의 ID
     * @return 그룹 정보
     */
        @Transactional(readOnly = true)
    public GroupsResponse getGroupAdmin(Long userId, Long groupId) {
        GroupMembers members = groupMembersService.validateGroupMembers(groupId, userId);

        // role이 member라면 권한 없음 에러를 던져주기
        if (members.getGroupRole() == GroupMembers.GroupRole.MEMBER) {
            throw NoPermissionException.forAdmin(members.getGroupMemberId());
        }

        return GroupsResponse.ofAdmin(members.getGroups());
    }

    /**
     * 시스템 관리자가 group List를 조회
     * @param userRole 로그인한 사용자의 권한...?
     * @param userId 로그인한 user ID
     * @return GroupList 반환
     * return 하기 전에 validateUserExists로 검증
     */
    @Transactional(readOnly = true)
    public List<GroupsListResponse> getGroupList(String userRole, Long userId) {
        // 유저가 존재하는지 검증
        groupMembersService.validateUserExists(userId);

        return groupService.getGroupList(userRole, userId);
    }




}
