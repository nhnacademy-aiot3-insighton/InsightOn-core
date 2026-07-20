package com.insighton.core.groupmember.service.impl;

import com.insighton.core.groupmember.dto.request.GroupMembersJoinRequest;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.exception.AlreadyJoinedException;
import com.insighton.core.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.groupmember.exception.UserIdNotFoundException;
import com.insighton.core.groupmember.repository.GroupMembersRepository;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.exception.GroupNotFoundException;
import com.insighton.core.groups.exception.NoPermissionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupMembersServiceImpl implements GroupMembersService{

    private final GroupMembersRepository groupMembersRepository;

    @Override
    @Transactional
    public void joinGroupByToken(GroupMembersJoinRequest request) {

        GroupMembers groupMembers = groupMembersRepository.findByUserId(request.userId())
                .orElseThrow(()-> new GroupMemberNotFoundException(request.userId()));

        // 이미 가입된 유저인지 체크
        groupMembersRepository.findByGroups_GroupIdAndUserId(groupMembers.getGroups().getGroupId(), request.userId())
                .orElseThrow(()->new AlreadyJoinedException(request.userId()));

        GroupMembers newMember = GroupMembers.builder()
                .groups(groupMembers.getGroups())
                .userId(request.userId())
                .groupRole(GroupMembers.GroupRole.MEMBER)
                .build();


        groupMembersRepository.save(newMember);
    }

    @Override
    @Transactional
    public void createGroupMember(Groups groups, Long userId){
        validateUserExists(userId);


        GroupMembers members = GroupMembers.builder()
                .groups(groups)
                .userId(userId)
                .groupRole(GroupMembers.GroupRole.SUPER_MANAGER)
                .build();

        groupMembersRepository.save(members);
    }

    @Override
    @Transactional
    public void toggleManagerRole(Long groupId, Long targetUserId, Long adminId) {
        // 1. group id가 존재하는지 확인
        GroupMembers adminMember = validateGroupMembers(groupId, adminId);
        GroupMembers targetMember = validateGroupMembers(groupId, targetUserId);

        // 2. adminID가 manager나 owner권한을 가진 자인지 확인
        // target의 권한이 member일 경우 member는 권한이 없음
        if(targetMember.isMember()){
            if(adminMember.isMember()){
                throw NoPermissionException.forAdmin(adminId);
            }
            // manager의 권한은 owner말고는 변경하지 못함
        } else if (targetMember.isManager()) {
            if(adminMember.isMember() || adminMember.isManager()){
                throw NoPermissionException.forAdmin(adminId);
            }
        }else {
            if(adminMember.isSuperManager()){
                throw NoPermissionException.forAdmin(adminId);
            }
        }
        // 3. targetUser가 Member권한일 때 Manager로 변경
        targetMember.updateRole(targetMember.getGroupRole() == GroupMembers.GroupRole.MEMBER ? GroupMembers.GroupRole.MANAGER : GroupMembers.GroupRole.MEMBER);
    }

    //     ==================== 공통 검증 및 조회용 헬퍼 메서드 ====================

    /**
     * 그룹 멤버의 권환 확인용
     */
    public boolean isGroupAdmin(Long groupId, Long userId){
        GroupMembers members = validateGroupMembers(groupId, userId);

        return members.getGroupRole() == GroupMembers.GroupRole.MANAGER || members.getGroupRole() == GroupMembers.GroupRole.SUPER_MANAGER;
    }
    /**
     * 특정 그룹에 속한 멤버인지 검증하고 멤버 객체 반환
     */
    @Override
    public GroupMembers validateGroupMembers(Long groupId, Long userId) {
        return groupMembersRepository.findByGroups_GroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new UserIdNotFoundException(userId));
    }

    /**
     * 시스템에 존재하는 올바른 유저인지 확인
     */
    @Override
    public void validateUserExists(Long userId) {
        groupMembersRepository.findByUserId(userId)
                .orElseThrow(() -> new UserIdNotFoundException(userId));
    }
}
