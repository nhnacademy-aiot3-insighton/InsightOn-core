package com.insighton.core.groupmember.service.impl;

import com.insighton.core.groupmember.client.AuthClient;
import com.insighton.core.groupmember.dto.request.GroupMembersJoinRequest;
import com.insighton.core.groupmember.dto.response.AuthUserResponse;
import com.insighton.core.groupmember.dto.response.GroupMembersListResponse;
import com.insighton.core.groupmember.dto.response.GroupMembersResponse;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.exception.*;
import com.insighton.core.groupmember.repository.GroupMembersRepository;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.groups.exception.UnAuthorizedAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GroupMembersServiceImpl implements GroupMembersService {

    private final GroupMembersRepository groupMembersRepository;
    private final AuthClient authClient;

    @Override
    @Transactional
    public void joinGroupByToken(Groups groups, GroupMembersJoinRequest request) {

        // 이 그룹이나 아니면 다른 그룹에라도(?) 이미 가입된 유저인지 체크 존재 안 해야 함
        validateUserNotInAnyGroup(request.userId());

        GroupMembers newMember = GroupMembers.builder()
                .groups(groups)
                .userId(request.userId())
                .groupRole(GroupMembers.GroupRole.MEMBER)
                .build();


        groupMembersRepository.save(newMember);
    }

    @Override
    @Transactional
    public void createGroupMember(Groups groups, Long userId) {
        // 이 그룹과 이미 다른 그룹에 존재하는 유저인지 체크 존재 안 해야 함
        validateUserNotInAnyGroup(userId);

        GroupMembers members = GroupMembers.builder()
                .groups(groups)
                .userId(userId)
                .groupRole(GroupMembers.GroupRole.SUPER_MANAGER)
                .build();

        groupMembersRepository.save(members);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMembersListResponse> getGroupMemberList(Long userId, Long groupId) {
        GroupMembers members = validateGroupMembers(groupId, userId);

        if (members.isMember()) {
            throw new UnAuthorizedAccessException(userId);
        }

        return groupMembersRepository.findAllByGroups_GroupId(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupMembersResponse getGroupMember(Long userId, Long groupId, Long groupMemberId) {

        GroupMembers requester = validateGroupMembers(groupId, userId);

        GroupMembers members = groupMembersRepository.findByGroupMemberIdAndGroups_GroupId(groupMemberId, groupId)
                .orElseThrow(() -> GroupMemberNotFoundException.byMemberIdAndGroupId(groupMemberId, groupId));

        if (requester.isMember()) {
            throw new UnAuthorizedAccessException(userId);
        }

        AuthUserResponse authUserResponse = authClient.getUserResponse(members.getUserId());

        return GroupMembersResponse.builder()
                .userId(members.getUserId())
                .groupRole(members.getGroupRole())
                .userName(authUserResponse.userName())
                .userPhoneNumber(authUserResponse.userPhoneNumber())
                .joinedAt(members.getJoinedAt())
                .build();
    }

    @Override
    @Transactional
    public void toggleManagerRole(Long groupId, Long targetGroupMemberId, Long adminId) {
        // 1. group id가 존재하는지 확인
        GroupMembers adminMember = validateGroupMembers(groupId, adminId);
        GroupMembers targetMember = groupMembersRepository.findByGroupMemberIdAndGroups_GroupId(targetGroupMemberId, groupId)
                .orElseThrow(() -> GroupMemberNotFoundException.byMemberIdAndGroupId(targetGroupMemberId, groupId));

        // 2. adminID가 manager나 owner권한을 가진 자인지 확인
        // target의 권한이 member일 경우 member는 권한이 없음
        if (targetMember.isMember()) {
            if (adminMember.isMember()) {
                throw NoPermissionException.forAdmin(adminId);
            }
        }

        // manager의 권한은 owner말고는 변경하지 못함
        if (targetMember.isManager()) {
            if (adminMember.isMember() || adminMember.isManager() || !adminMember.isSuperManager()) {
                throw NoPermissionException.forAdmin(adminId);
            }
        }

        // super manager는 권한 변경 X
        if (targetMember.isSuperManager()) {
            throw NoPermissionException.forAdmin(adminId);
        }

        // 3. targetUser가 Member권한일 때 Manager로 변경
        targetMember.updateRole(targetMember.getGroupRole() == GroupMembers.GroupRole.MEMBER ? GroupMembers.GroupRole.MANAGER : GroupMembers.GroupRole.MEMBER);
    }

    @Override
    @Transactional
    public void toggleSuperManagerRole(Long groupId, Long targetGroupMemberId, Long superManagerUserId) {
        // 1. group id가 존재하는지 확인
        GroupMembers superManager = validateGroupMembers(groupId, superManagerUserId);
        GroupMembers targetMember = groupMembersRepository.findByGroupMemberIdAndGroups_GroupId(targetGroupMemberId, groupId)
                .orElseThrow(() -> GroupMemberNotFoundException.byMemberIdAndGroupId(targetGroupMemberId, groupId));

        // super Manager가 아니면 안 됨!!
        if (!superManager.isSuperManager()) {
            throw new ManagerRoleRequiredForTransferException(superManager.getGroupMemberId());
        }

        // super Manager이지만 대상이 일반 Member일 경우에는 양도 ㄴㄴ
        if (superManager.isSuperManager()) {
            if (targetMember.isMember() || !targetMember.isManager()) {
                throw new ManagerRoleRequiredForTransferException();
            }
        }

        targetMember.updateRole(GroupMembers.GroupRole.SUPER_MANAGER);
    }

    @Override
    @Transactional
    public void kickGroupMember(Long adminId, Long groupId, Long targetGroupMemberId) {

        GroupMembers admin = validateGroupMembers(groupId, adminId);
        GroupMembers target = groupMembersRepository.findByGroupMemberIdAndGroups_GroupId(targetGroupMemberId, groupId)
                .orElseThrow(() -> GroupMemberNotFoundException.byMemberIdAndGroupId(targetGroupMemberId, groupId));

        // 삭제를 시도하는 자가 member 권한이면 아무도 삭제할 수 없음
        if (admin.isMember()) {
            throw NoPermissionException.forAdmin(admin.getGroupMemberId());
        }

        // 삭제를 시도하는자가 target과 동일인물이면 안됨
        if (Objects.equals(admin.getUserId(), target.getUserId())) {
            throw new SuperManagerCannotLeaveException(admin.getGroupMemberId());
        }

        // 삭제하려는 자가 manager권한인데 target이 같은 권한이거나 superManager면 삭제할 수 없음
        if (admin.isManager()) {
            if (target.isManager() || target.isSuperManager()) {
                throw NoPermissionException.forAdmin(admin.getGroupMemberId());
            }
        }


        groupMembersRepository.delete(target);
    }

    @Override
    @Transactional
    public void deleteGroupMemberAll(Long adminId, Long groupId) {

        GroupMembers admin = validateGroupMembers(groupId, adminId);

        // superManager 아니면 아예 안돼
        if (admin.isManager() || admin.isMember() || !admin.isSuperManager()) {
            throw new UnAuthorizedAccessException(adminId);
        }

        // 그들이 속한 그룹(삭제될 예정임) ID로 찾아서 모두 삭제
        groupMembersRepository.deleteAllByGroups_GroupId(groupId);
    }

    @Override
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {

        GroupMembers members = validateGroupMembers(groupId, userId);

        // super manager는 권한 위임 전에는 절대 탈퇴 불가
        if (members.isSuperManager()) {
            throw new SuperManagerCannotLeaveException(members.getGroupMemberId());
        }

        groupMembersRepository.delete(members);
    }

    //     ==================== 공통 검증 및 조회용 헬퍼 메서드 ====================

    /**
     * 그룹 멤버의 권환 확인용
     */
    @Override
    public boolean isGroupAdmin(Long groupId, Long userId) {
        GroupMembers members = validateGroupMembers(groupId, userId);

        return members.isManager() || members.isSuperManager();
    }

    /**
     * 특정 그룹에 속한 멤버인지 검증하고 멤버 객체 반환
     */
    @Override
    public GroupMembers validateGroupMembers(Long groupId, Long userId) {
        return groupMembersRepository.findByGroups_GroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));
    }

    /**
     * group에 존재하는 올바른 유저면 통과
     */
    @Override
    public void validateUserInAnyGroup(Long userId) {
        if (groupMembersRepository.existsByUserId(userId)) {
            return;
        }
        throw new NotJoinedAnyGroupException(userId);
    }

    /**
     * 어떤 그룹에도 존재하지 않아야 통과
     */
    @Override
    public void validateUserNotInAnyGroup(Long userId) {
        if (groupMembersRepository.existsByUserId(userId)) {
            throw new AlreadyJoinedException(userId);
        }
    }
}
