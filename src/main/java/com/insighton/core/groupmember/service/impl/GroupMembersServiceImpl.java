package com.insighton.core.groupmember.service.impl;

import com.insighton.core.groupmember.dto.request.GroupMembersJoinRequest;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.exception.AlreadyJoinedException;
import com.insighton.core.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.groupmember.exception.UserIdNotFoundException;
import com.insighton.core.groupmember.repository.GroupMembersRepository;
import com.insighton.core.groupmember.service.GroupMembersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupMembersServiceImpl implements GroupMembersService{

    private final GroupMembersRepository groupMembersRepository;

    /**
     * 그룹 가입
     * @param request 그룹 가입 요청 DTO
     */
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

//     ==================== 공통 검증 및 조회용 헬퍼 메서드 ====================

    /**
     * 특정 그룹에 속한 멤버인지 검증하고 멤버 객체 반환
     */
    @Override
    public GroupMembers validateGroupMembers(Long groupId, Long userId) {
        // findByGroups_GroupIdAndUserId의 파라미터 바인딩 순서(groupId, userId) 오류를 바로잡았습니다.
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
