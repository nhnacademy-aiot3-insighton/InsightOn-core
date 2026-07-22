package com.insighton.core.groups.service;

import com.insighton.core.groupmember.dto.request.GroupMembersJoinRequest;
import com.insighton.core.groupmember.dto.response.GroupMembersListResponse;
import com.insighton.core.groups.dto.request.GroupsCreateRequest;
import com.insighton.core.groups.dto.request.GroupsUpdateRequest;
import com.insighton.core.groups.dto.response.GroupsListResponse;
import com.insighton.core.groups.dto.response.GroupsResponse;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.exception.UnAuthorizedAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupManagementUseCase {
    private final GroupsService groupService;
    private final GroupMembersService groupMembersService;

    // ====================== Group Controller ======================

    /**
     * 그룹 가입
     * @param request 그룹 가입 요청 DTO
     */
    @Transactional
    public void joinGroupByToken(GroupMembersJoinRequest request) {
        // inviteToken으로 대상 그룹이 존재하는지 확인 및 조회
        Groups groups = groupService.validateGroupByInviteToken(request.inviteToken());

        groupMembersService.joinGroupByToken(groups, request);
    }

    /**
     * 그룹 생성 시 SUPER_MANAGER로 등록되는
     * @param groupsCreateRequest 그룹 생성 요청 정보
     * @param userId 그룹을 생성하는 user의 ID
     */
    @Transactional
    public void createGroup(GroupsCreateRequest groupsCreateRequest, Long userId){
        Groups groups = groupService.createGroup(groupsCreateRequest);

        groupMembersService.createGroupMember(groups, userId);
    }


    /**
     * 그룹 수정
     * @param request Group 수정 요청 정보
     * @param userId login한 user의 ID
     * @param groupId 수정하려는 group의 ID
     */
    @Transactional
    public void updateGroup(GroupsUpdateRequest request, Long userId, Long groupId){
        if(groupMembersService.isGroupAdmin(groupId, userId)){
            groupService.updateGroup(request, groupId);
            return;
        }
        throw new UnAuthorizedAccessException(userId);
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
        // 유저가 존재하는지 검증(다른 그룹에 가입이 안 되어있어야함 )
        groupMembersService.validateUserNotInAnyGroup(userId);

        return groupService.getGroupPreview(inviteToken, groupId);
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
        if (members.isManager() || members.isSuperManager()) {
            return GroupsResponse.ofAdmin(members.getGroups());
        }

        // 대상 그룹 조회 (없을 시 exception 던지기)
        return GroupsResponse.ofPublic(members.getGroups());
    }


    /**
     * 토큰 재발급
     * @param groupId 재발급 하려는 group의 ID
     * @param userId 재발급 하려는 user의 ID
     */
    @Transactional
    public void newInviteToken(Long userId, Long groupId){
        GroupMembers groupMembers = groupMembersService.validateGroupMembers(groupId, userId);

        if(groupMembers.isMember()){
            throw NoPermissionException.forAdmin(groupMembers.getGroupMemberId());
        }
        groupService.newInviteToken(groupId);
    }

    /**
     * 그룹 삭제
     * @param userId 그룺을 삭제할 권한을 가진 userID
     * @param groupId 삭제될 group ID
     */
    @Transactional
    public void deleteGroup(Long userId, Long groupId){
        GroupMembers groupMembers = groupMembersService.validateGroupMembers(groupId, userId);

        if(!groupMembers.isSuperManager()){
            throw NoPermissionException.forAdmin(groupMembers.getGroupMemberId());
        }

        groupMembersService.deleteGroupMemberAll(userId, groupId);

        groupService.deleteGroup(groupId);
    }
}
