package com.insighton.core.service.groupmember.impl;

import com.insighton.core.dto.groupmember.request.GroupMemberJoinRequest;
import com.insighton.core.entity.groupmember.GroupMembers;
import com.insighton.core.exception.groupmember.AlreadyJoinedException;
import com.insighton.core.repository.groupmember.GroupMembersRepository;
import com.insighton.core.service.groupmember.GroupMembersService;
import com.insighton.core.entity.groups.Groups;
import com.insighton.core.exception.groups.InviteTokenNotFoundException;
import com.insighton.core.repository.groups.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupMembersServiceImpl implements GroupMembersService{

    private final GroupRepository groupRepository;
    private final GroupMembersRepository groupMembersRepository;

    @Override
    @Transactional
    public void joinGroupByToken(GroupMemberJoinRequest request) {
        // inviteToken으로 대상 그룹이 존재하는지 확인 및 조회
        Groups group = groupRepository.findByInviteToken(request.inviteToken())
                .orElseThrow(() -> new InviteTokenNotFoundException(request.inviteToken()));

        // 이미 가입된 유저인지 체크
        groupMembersRepository.findByGroups_GroupIdAndUserId(group.getGroupId(), request.userId())
                .orElseThrow(()->new AlreadyJoinedException(request.userId()));


        GroupMembers newMember = GroupMembers.builder()
                .groups(group)
                .userId(request.userId())
                .groupRole(GroupMembers.GroupRole.MEMBER)
                .build();

        groupMembersRepository.save(newMember);
    }


}
