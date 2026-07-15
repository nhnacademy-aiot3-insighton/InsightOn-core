package com.insighton.core.groups.service.impl;

import com.insighton.core.groups.dto.request.GroupCreateRequest;
import com.insighton.core.groups.dto.response.GroupResponse;
import com.insighton.core.groups.entity.GroupsEntity;
import com.insighton.core.groups.exception.GroupNotFoundException;
import com.insighton.core.groups.exception.InviteTokenNotFoundException;
import com.insighton.core.groups.repository.GroupRepository;
import com.insighton.core.groups.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    
    private final GroupRepository repository;

    @Override
    @Transactional
    public void createGroup(GroupCreateRequest request) {
        // 초대 토큰 랜덤 발급 (UUID 기반으로 대시(-)를 제외한 32자리 고유문자 생성 후 12자리로 자르기)
        String inviteToken = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // 그룹 엔티티 생성 및 기본 정보 세팅
        // (우선순위: request 객체의 값이 존재하면 사용하고, 없는 경우 개별 파라미터 값을 활용합니다.)
        GroupsEntity group = GroupsEntity.builder()
                .name(request.name())
                .description(request.description())
                .location(request.location())
                .inviteToken(inviteToken)
                .build();


        repository.save(group);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroupByInviteToken(String inviteToken) {
        // 대상 그룹 조회 (없을 시 exception 던지기)
        GroupsEntity groupsEntity = repository.findByInviteToken(inviteToken)
                .orElseThrow(()-> new InviteTokenNotFoundException(inviteToken));

        return GroupResponse.ofAdmin(groupsEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroupByToken(String inviteToken) {
        // 대상 그룹 조회 (없을 시 exception 던지기)
        GroupsEntity groupsEntity = repository.findByInviteToken(inviteToken)
                .orElseThrow(()-> new InviteTokenNotFoundException(inviteToken));

        return GroupResponse.ofPublic(groupsEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getMyGroup(Long groupId) {
        // 대상 그룹 조회 (없을 시 exception 던지기)
        GroupsEntity groupsEntity = repository.findById(groupId)
                .orElseThrow(()-> new GroupNotFoundException(groupId));

        return GroupResponse.ofPublic(groupsEntity);
    }

    @Override
    @Transactional
    public void newInviteToken(Long groupId) {
        // 대상 그룹 조회 (없을 시 exception 던지기)
        GroupsEntity groupsEntity = repository.findById(groupId)
                .orElseThrow(()-> new GroupNotFoundException(groupId));

        // 새로운 12자리 토큰 생성 및 업데이트
        String newToken = UUID.randomUUID().toString().replace("-", "").substring(0,12);

        groupsEntity.rotateInviteToken(newToken);
    }

    @Override
    public void deleteGroup(Long groupId) {
        GroupsEntity groupsEntity = repository.findById(groupId)
                .orElseThrow(()-> new GroupNotFoundException(groupId));

        repository.delete(groupsEntity);
    }
}
