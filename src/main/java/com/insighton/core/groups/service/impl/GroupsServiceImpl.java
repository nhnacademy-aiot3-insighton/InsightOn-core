package com.insighton.core.groups.service.impl;

import com.insighton.core.groups.dto.request.GroupsCreateRequest;
import com.insighton.core.groups.dto.request.GroupsUpdateRequest;
import com.insighton.core.groups.dto.response.GroupsListResponse;
import com.insighton.core.groups.dto.response.GroupsResponse;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.exception.GroupNotFoundException;
import com.insighton.core.groups.exception.InviteTokenNotFoundException;
import com.insighton.core.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.groups.repository.GroupsRepository;
import com.insighton.core.groups.service.GroupsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupsServiceImpl implements GroupsService {
    private final GroupsRepository groupsRepository;

    @Override
    @Transactional
    public Groups createGroup(GroupsCreateRequest request) {
        // 초대 토큰 랜덤 발급 (UUID 기반으로 대시(-)를 제외한 32자리 고유문자 생성 후 12자리로 자르기)
        String inviteToken = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // 그룹 엔티티 생성 및 기본 정보 세팅
        // (우선순위: request 객체의 값이 존재하면 사용하고, 없는 경우 개별 파라미터 값을 활용합니다.)
        Groups group = Groups.builder()
                .name(request.name())
                .description(request.description())
                .location(request.location())
                .inviteToken(inviteToken)
                .build();

        return groupsRepository.save(group);
    }

    @Override
    @Transactional
    public void updateGroup(GroupsUpdateRequest request, Long groupId) {

        Groups groups = groupFindById(groupId);

        groups.update(request);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupsResponse getGroupPreview(String inviteToken, Long groupId) {
        // token으로 대상 그룹 조회 (token이 존재하지 않을 시 exception 던지기)
        Groups groupsEntity = groupsRepository.findByInviteTokenAndGroupId(inviteToken, groupId)
                .orElseThrow(InviteTokenNotFoundException::new);

        return GroupsResponse.ofPublic(groupsEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupsListResponse> getGroupList(String userRole, Long userId) {

        if ("ADMIN".equals(userRole)) {
            return groupsRepository.findAll().stream()
                    .map(group -> new GroupsListResponse(
                            group.getGroupId(),
                            group.getName(),
                            group.getDescription(),
                            group.getLocation()
                    ))
                    .toList();
        }
        // 시스템 관리자가 아닐 경우에 접근권한에러 던지기
        throw new UnAuthorizedAccessException(userId);
    }

    @Override
    @Transactional
    public void newInviteToken(Long groupId) {
        // 대상 그룹 조회 (없을 시 exception 던지기)
        Groups groupsEntity = groupFindById(groupId);

        // 새로운 12자리 토큰 생성 및 업데이트
        String newToken = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        groupsEntity.rotateInviteToken(newToken);
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        // 대상 그룹 조회 (없을 시 exception 던지기)
        Groups groupsEntity = groupFindById(groupId);

        groupsRepository.delete(groupsEntity);
    }




    // ==================== 공통 검증 및 조회용 헬퍼 메서드 ====================

    /**
     * 초대 토큰으로 group이 존재하는지 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Groups validateGroupByInviteToken(String inviteToken){
        // inviteToken으로 대상 그룹이 존재하는지 확인 및 조회
        return groupsRepository.findByInviteToken(inviteToken)
                .orElseThrow(InviteTokenNotFoundException::new);
    }

    /**
     * ID로 그룹이 존재하는지 조회 (존재하지 않을 시 예외 발생)
     */
    @Override
    @Transactional
    public Groups groupFindById(Long groupId) {
        return groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
    }
}