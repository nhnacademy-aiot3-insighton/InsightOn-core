package com.insighton.core.domain.groups.service.impl;

import com.insighton.core.domain.groups.dto.request.GroupRequest;
import com.insighton.core.domain.groups.dto.response.GroupAdminResponse;
import com.insighton.core.domain.groups.dto.response.GroupResponse;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.exception.GroupNotFoundException;
import com.insighton.core.domain.groups.exception.InvitationTokenMismatchException;
import com.insighton.core.domain.groups.exception.InviteTokenNotFoundException;
import com.insighton.core.domain.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.domain.groups.repository.GroupRepository;
import com.insighton.core.domain.groups.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public Group createGroup(GroupRequest request) {
        // 초대 토큰 랜덤 발급 (UUID 기반으로 대시(-)를 제외한 32자리 고유문자 생성 후 12자리로 자르기)
        String inviteToken = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        // 그룹 엔티티 생성 및 기본 정보 세팅
        // (우선순위: request 객체의 값이 존재하면 사용하고, 없는 경우 개별 파라미터 값을 활용합니다.)
        Group group = Group.builder()
                .name(request.name())
                .description(request.description())
                .groupRegion(request.groupRegion())
                .inviteToken(inviteToken)
                .build();

        Group savedGroup = groupRepository.save(group);
        log.info("그룹 생성 완료 - groupId: {}, name: {}", savedGroup.getGroupId(), savedGroup.getName());
        return savedGroup;
    }

    @Override
    @Transactional
    public void updateGroup(GroupRequest request, Long groupId) {

        Group group = groupFindById(groupId);

        group.update(request);
        log.info("그룹 정보 수정 완료 - groupId: {}", groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroupPreview(String inviteToken, Long groupId) {
        // token으로 대상 그룹 조회 (token이 존재하지 않을 시 exception 던지기)
        Group groupEntity = groupRepository.findByInviteTokenAndGroupId(inviteToken, groupId)
                .orElseThrow(InviteTokenNotFoundException::new);

        return GroupResponse.ofPublic(groupEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GroupAdminResponse> getGroupList(String userRole, Long userId, Pageable pageable) {

        if ("ADMIN".equals(userRole)) {

            return groupRepository.findAll(pageable)
                    .map(group -> new GroupAdminResponse(
                            group.getGroupId(),
                            group.getName(),
                            group.getDescription(),
                            group.getGroupRegion()
                    ));
        }
        // 시스템 관리자가 아닐 경우에 접근권한에러 던지기
        throw new UnAuthorizedAccessException(userId);
    }

    @Override
    @Transactional
    public void newInviteToken(Long groupId) {
        // 대상 그룹 조회 (없을 시 exception 던지기)
        Group groupEntity = groupFindById(groupId);

        // 새로운 12자리 토큰 생성 및 업데이트
        String newToken = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        groupEntity.rotateInviteToken(newToken);
        log.info("그룹 초대 토큰 재발급 완료 - groupId: {}", groupId);
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        // 대상 그룹 조회 (없을 시 exception 던지기)
        Group groupEntity = groupFindById(groupId);


        groupRepository.delete(groupEntity);
        log.info("그룹 삭제 완료 - groupId: {}", groupId);
    }


    // ==================== 공통 검증 및 조회용 헬퍼 메서드 ====================

    /**
     * 초대 토큰으로 group이 존재하는지 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Group validateGroupByInviteToken(String inviteToken) {
        // inviteToken으로 대상 그룹이 존재하는지 확인 및 조회
        return groupRepository.findByInviteToken(inviteToken)
                .orElseThrow(InviteTokenNotFoundException::new);
    }

    /**
     * ID로 그룹이 존재하는지 조회 (존재하지 않을 시 예외 발생)
     */
    @Override
    public Group groupFindById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
    }

    @Override
    @Transactional(readOnly = true)
    public void validateInviteToken(Long groupId, String inviteToken) {
        Group groupEntity = groupFindById(groupId);
        if (!Objects.equals(groupEntity.getInviteToken(), inviteToken)) {
            throw new InvitationTokenMismatchException();
        }
    }

    @Override
    @Transactional
    public Group findWithLockByGroupId(Long groupId) {
        return groupRepository.findWithLockByGroupId(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
    }
}