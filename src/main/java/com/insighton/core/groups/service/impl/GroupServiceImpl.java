package com.insighton.core.groups.service.impl;

import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.exception.UserIdNotFoundException;
import com.insighton.core.groupmember.repository.GroupMembersRepository;
import com.insighton.core.groups.dto.request.GroupCreateRequest;
import com.insighton.core.groups.dto.response.GroupListResponse;
import com.insighton.core.groups.dto.response.GroupResponse;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.exception.GroupNotFoundException;
import com.insighton.core.groups.exception.InviteTokenNotFoundException;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.groups.repository.GroupRepository;
import com.insighton.core.groups.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    private final GroupMembersRepository groupMembersRepository;
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public void createGroup(GroupCreateRequest request, Long userId) {
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

        groupRepository.save(group);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroupAdmin(Long userId, Long groupId) {
        GroupMembers members = validateGroupMembers(groupId, userId);

        // role이 member라면 권한 없음 에러를 던져주기
        if (members.getGroupRole() == GroupMembers.GroupRole.MEMBER) {
            throw NoPermissionException.forAdmin(members.getGroupMemberId());
        }

        return GroupResponse.ofAdmin(members.getGroups());
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroupPreview(String inviteToken, Long userId, Long groupId) {
        // 유저가 존재하는지 검증
        validateUserExists(userId);

        // token으로 대상 그룹 조회 (token이 존재하지 않을 시 exception 던지기)
        Groups groupsEntity = groupRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new InviteTokenNotFoundException(inviteToken));

        return GroupResponse.ofPublic(groupsEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponse getMyGroup(Long userId, Long groupId) {
        // 이건 group안에 user가 속해있는지 같이 보기 위한 메서드임...
        GroupMembers members = validateGroupMembers(groupId, userId);

        // 관리자나 그룹 생성자일 때는
        if (members.getGroupRole() == GroupMembers.GroupRole.MANAGER || members.getGroupRole() == GroupMembers.GroupRole.OWNER) {
            throw NoPermissionException.forResource(members.getGroupMemberId());
        }

        // 대상 그룹 조회 (없을 시 exception 던지기)
        return GroupResponse.ofPublic(members.getGroups());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupListResponse> getGroupList(String userRole, Long userId) {

        validateUserExists(userId);

        if ("ADMIN".equals(userRole)) {
            return groupRepository.findAll().stream()
                    .map(group -> new GroupListResponse(
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
        Groups groupsEntity = getGroupOrThrow(groupId);

        // 새로운 12자리 토큰 생성 및 업데이트
        String newToken = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        groupsEntity.rotateInviteToken(newToken);
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        // 대상 그룹 조회 (없을 시 exception 던지기)
        Groups groupsEntity = getGroupOrThrow(groupId);

        groupRepository.delete(groupsEntity);
    }

    // ==================== 공통 검증 및 조회용 헬퍼 메서드 ====================

    /**
     * 특정 그룹에 속한 멤버인지 검증하고 멤버 객체 반환
     */
    private GroupMembers validateGroupMembers(Long groupId, Long userId) {
        // findByGroups_GroupIdAndUserId의 파라미터 바인딩 순서(groupId, userId) 오류를 바로잡았습니다.
        return groupMembersRepository.findByGroups_GroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new UserIdNotFoundException(userId));
    }

    /**
     * 단건 그룹 조회 (존재하지 않을 시 예외 발생)
     */
    private Groups getGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));
    }

    /**
     * 시스템에 존재하는 올바른 유저인지 확인
     */
    private void validateUserExists(Long userId) {
        groupMembersRepository.findByUserId(userId)
                .orElseThrow(() -> new UserIdNotFoundException(userId));
    }
}