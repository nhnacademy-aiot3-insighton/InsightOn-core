package com.insighton.core.domain.groupmember.service.impl;

import com.insighton.core.adapter.client.internal.AuthClient;
import com.insighton.core.domain.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.domain.groupmember.dto.response.AuthUserResponse;
import com.insighton.core.domain.groupmember.dto.response.GroupMemberListResponse;
import com.insighton.core.domain.groupmember.dto.response.GroupMemberResponse;
import com.insighton.core.domain.groupmember.dto.response.UserGroupResponse;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.exception.*;
import com.insighton.core.domain.groupmember.repository.GroupMemberRepository;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.groups.exception.UnAuthorizedAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;
    private final AuthClient authClient;

    @Override
    @Transactional
    public void joinGroupByToken(Group group, GroupMemberJoinRequest request) {

        // 이 그룹이나 아니면 다른 그룹에라도(?) 이미 가입된 유저인지 체크 존재 안 해야 함
        validateUserNotInAnyGroup(request.userId());

        GroupMember newMember = GroupMember.builder()
                .group(group)
                .userId(request.userId())
                .groupRole(GroupMember.GroupRole.MEMBER)
                .build();


        groupMemberRepository.save(newMember);
        log.info("초대 토큰 그룹 가입 완료 - userId: {}, groupId: {}", request.userId(), group.getGroupId());
    }

    @Override
    @Transactional
    public void createGroupMember(Group group, Long userId) {
        // 이 그룹과 이미 다른 그룹에 존재하는 유저인지 체크 존재 안 해야 함
        validateUserNotInAnyGroup(userId);

        GroupMember members = GroupMember.builder()
                .group(group)
                .userId(userId)
                .groupRole(GroupMember.GroupRole.SUPER_MANAGER)
                .build();

        groupMemberRepository.save(members);
        log.info("그룹 생성자(SUPER_MANAGER) 등록 완료 - userId: {}, groupId: {}", userId, group.getGroupId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberListResponse> getGroupMemberList(Long userId, Long groupId) {
        log.debug("그룹 멤버 목록 조회 요청 - userId: {}, groupId: {}", userId, groupId);
        GroupMember requester = validateGroupMembers(groupId, userId);

        if (requester.isMember()) {
            log.warn("그룹 멤버 목록 조회 권한 없음 - userId: {}, groupId: {}", userId, groupId);
            throw new UnAuthorizedAccessException(userId);
        }

        List<GroupMember> memberList = groupMemberRepository.findByGroupGroupId(groupId);

        List<GroupMemberListResponse> result = memberList.stream()
                .map(gm -> {
                    String userName = "알 수 없음";
                    try {
                        AuthUserResponse authUser = authClient.getUserResponse(gm.getUserId());
                        if (authUser != null && authUser.userName() != null) {
                            userName = authUser.userName();
                        }
                    } catch (Exception e) {
                        log.warn("Auth 유저 정보 조회 실패 - userId: {}", gm.getUserId(), e);
                    }
                    return GroupMemberListResponse.builder()
                            .groupMemberId(gm.getGroupMemberId())
                            .userId(userId)
                            .userName(userName)
                            .groupRole(gm.getGroupRole())
                            .build();
                })
                .toList();

        log.info("그룹 멤버 목록 조회 완료 - groupId: {}, count: {}", groupId, result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public GroupMemberResponse getGroupMember(Long userId, Long groupId, Long groupMemberId) {
        log.debug("그룹 멤버 단건 조회 요청 - userId: {}, groupId: {}, groupMemberId: {}", userId, groupId, groupMemberId);

        GroupMember requester = validateGroupMembers(groupId, userId);

        GroupMember members = groupMemberRepository.findByGroupMemberIdAndGroupGroupId(groupMemberId, groupId)
                .orElseThrow(() -> GroupMemberNotFoundException.byMemberIdAndGroupId(groupMemberId, groupId));

        boolean isSelf = Objects.equals(requester.getUserId(), members.getUserId());

        // 둘 다 해당하지 않으면 에러
        if (requester.isMember() && !isSelf) {
            log.warn("그룹 멤버 단건 조회 권한 없음 - userId: {}, groupMemberId: {}", userId, groupMemberId);
            throw new UnAuthorizedAccessException(userId);
        }

        AuthUserResponse authUserResponse = authClient.getUserResponse(members.getUserId());

        log.info("그룹 멤버 단건 조회 완료 - groupMemberId: {}, targetUserId: {}", groupMemberId, members.getUserId());
        return GroupMemberResponse.builder()
                .userId(members.getUserId())
                .groupId(members.getGroup().getGroupId())
                .groupRole(members.getGroupRole())
                .userName(authUserResponse.userName())
                .userPhoneNumber(authUserResponse.userPhoneNumber())
                .joinedAt(members.getJoinedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GroupMemberResponse getGroupMemberAI(Long userId, Long groupId) {
        log.debug("AI용 그룹 멤버 조회 요청 - userId: {}, groupId: {}", userId, groupId);
        GroupMember members = validateGroupMembers(groupId, userId);

        log.info("AI용 그룹 멤버 조회 완료 - userId: {}, groupId: {}, role: {}", userId, groupId, members.getGroupRole());
        return GroupMemberResponse.builder()
                .userId(members.getUserId())
                .groupId(members.getGroup().getGroupId())
                .groupRole(members.getGroupRole())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getMyGroupId(Long userId) {
        log.debug("내 그룹 ID 조회 요청 - userId: {}", userId);
        GroupMember member = groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> GroupMemberNotFoundException.byUserId(userId));
        log.info("내 그룹 ID 조회 완료 - userId: {}, groupId: {}", userId, member.getGroup().getGroupId());
        return member.getGroup().getGroupId();
    }

    @Override
    @Transactional
    public void toggleManagerRole(Long groupId, Long targetGroupMemberId, Long adminId) {
        // 1. group id가 존재하는지 확인
        GroupMember adminMember = validateGroupMembers(groupId, adminId);
        GroupMember targetMember = groupMemberRepository.findByGroupMemberIdAndGroupGroupId(targetGroupMemberId, groupId)
                .orElseThrow(() -> GroupMemberNotFoundException.byMemberIdAndGroupId(targetGroupMemberId, groupId));

        // 2. adminID가 manager나 owner권한을 가진 자인지 확인
        // target의 권한이 member일 경우 member는 권한이 없음
        if (targetMember.isMember() && adminMember.isMember()) {
            throw NoPermissionException.forAdmin(adminId);
        }

        // manager의 권한은 owner말고는 변경하지 못함
        if (targetMember.isManager() && !adminMember.isSuperManager()) {
            throw NoPermissionException.forAdmin(adminId);
        }

        // super manager는 권한 변경 X
        if (targetMember.isSuperManager()) {
            throw NoPermissionException.forAdmin(adminId);
        }

        // 3. targetUser가 Member권한일 때 Manager로 변경
        GroupMember.GroupRole newRole = targetMember.getGroupRole() == GroupMember.GroupRole.MEMBER
                ? GroupMember.GroupRole.MANAGER : GroupMember.GroupRole.MEMBER;
        targetMember.updateRole(newRole);
        log.info("매니저 역할 토글 완료 - adminId: {}, targetGroupMemberId: {}, groupId: {}, newRole: {}",
                adminId, targetGroupMemberId, groupId, newRole);
    }

    @Override
    @Transactional
    public void toggleSuperManagerRole(Long groupId, Long targetGroupMemberId, Long superManagerUserId) {
        // 1. group id가 존재하는지 확인
        GroupMember superManager = validateGroupMembers(groupId, superManagerUserId);
        GroupMember targetMember = groupMemberRepository.findByGroupMemberIdAndGroupGroupId(targetGroupMemberId, groupId)
                .orElseThrow(() -> GroupMemberNotFoundException.byMemberIdAndGroupId(targetGroupMemberId, groupId));

        // super Manager가 아니면 안 됨!!
        if (!superManager.isSuperManager()) {
            throw new ManagerRoleRequiredForTransferException(superManager.getGroupMemberId());
        }

        // super Manager이지만 대상이 일반 Member일 경우에는 양도 ㄴㄴ
        if (targetMember.isMember() || !targetMember.isManager()) {
            throw new ManagerRoleRequiredForTransferException();
        }
        superManager.updateRole(GroupMember.GroupRole.MANAGER);
        targetMember.updateRole(GroupMember.GroupRole.SUPER_MANAGER);
        log.info("슈퍼매니저 권한 위임 완료 - 기존 superManagerUserId: {}, 새 superManagerGroupMemberId: {}, groupId: {}",
                superManagerUserId, targetGroupMemberId, groupId);
    }

    @Override
    @Transactional
    public void kickGroupMember(Long adminId, Long groupId, Long targetGroupMemberId) {

        GroupMember admin = validateGroupMembers(groupId, adminId);
        GroupMember target = groupMemberRepository.findByGroupMemberIdAndGroupGroupId(targetGroupMemberId, groupId)
                .orElseThrow(() -> GroupMemberNotFoundException.byMemberIdAndGroupId(targetGroupMemberId, groupId));

        // 삭제를 시도하는 자가 member 권한이면 아무도 삭제할 수 없음
        if (admin.isMember()) {
            throw NoPermissionException.forAdmin(admin.getGroupMemberId());
        }

        // 삭제를 시도하는자가 target과 동일인물이면 안됨
        if (Objects.equals(admin.getUserId(), target.getUserId())) {
            throw new CannotKickSelfException(admin.getGroupMemberId());
        }

        // 삭제하려는 자가 manager권한인데 target이 같은 권한이거나 superManager면 삭제할 수 없음
        if (admin.isManager() && (target.isManager() || target.isSuperManager())) {
            throw NoPermissionException.forAdmin(admin.getGroupMemberId());
        }


        groupMemberRepository.delete(target);
        log.info("그룹 멤버 강퇴 완료 - adminId: {}, targetMemberId: {}, groupId: {}", adminId, targetGroupMemberId, groupId);
    }

    @Override
    @Transactional
    public void deleteGroupMemberAll(Long adminId, Long groupId) {

        GroupMember admin = validateGroupMembers(groupId, adminId);

        // superManager 아니면 아예 안돼
        if (admin.isManager() || admin.isMember() || !admin.isSuperManager()) {
            throw new UnAuthorizedAccessException(adminId);
        }

        // 그들이 속한 그룹(삭제될 예정임) ID로 찾아서 모두 삭제
        groupMemberRepository.deleteAllByGroupGroupId(groupId);
        log.info("그룹 모든 멤버 삭제 완료 - adminId: {}, groupId: {}", adminId, groupId);
    }

    @Override
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {

        GroupMember members = validateGroupMembers(groupId, userId);

        // super manager는 권한 위임 전에는 절대 탈퇴 불가
        if (members.isSuperManager()) {
            throw new SuperManagerCannotLeaveException(members.getGroupMemberId());
        }

        groupMemberRepository.delete(members);
        log.info("그룹 탈퇴 완료 - userId: {}, groupId: {}", userId, groupId);
    }

    //     ==================== 공통 검증 및 조회용 헬퍼 메서드 ====================

    /**
     * 그룹 멤버의 권환 확인용
     */
    @Override
    public boolean isGroupAdmin(Long groupId, Long userId) {
        GroupMember members = validateGroupMembers(groupId, userId);

        return members.isManager() || members.isSuperManager();
    }

    /**
     * group member를 가져와서 권한이 있는지 확인하는 method
     */
    @Override
    @Transactional
    public GroupMember validateGroupAdmin(Long groupId, Long userId) {
        GroupMember member = validateGroupMembers(groupId, userId);
        if (member.isMember()) {
            throw NoPermissionException.forAdmin(member.getGroupMemberId());
        }
        return member;
    }


    /**
     * Auth 요청
     */
    @Override
    @Transactional
    public UserGroupResponse userGroupAuth(Long userId) {
        log.debug("유저 그룹 소속/권한 확인 요청 (Auth) - userId: {}", userId);
        GroupMember member = groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> GroupMemberNotFoundException.byUserId(userId));

        boolean isGroup = groupMemberRepository.existsByUserId(userId);

        String groupName = member.getGroup().getName();

        log.info("유저 그룹 소속/권한 확인 완료 (Auth) - userId: {}, groupName: {}, isGroup: {}", userId, groupName, isGroup);
        return new UserGroupResponse(isGroup, groupName);
    }

    /**
     * Auth 요청
     */
    @Transactional
    public boolean existsManagerGroupAuth(Long userId) {
        log.debug("매니저 권한 여부 확인 요청 (Auth) - userId: {}", userId);
        GroupMember member = groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> GroupMemberNotFoundException.byUserId(userId));

        boolean result = member.isManager() || member.isSuperManager();
        log.info("매니저 권한 여부 확인 완료 (Auth) - userId: {}, isManager: {}", userId, result);
        return result;
    }

    /**
     * 특정 그룹에 속한 멤버인지 검증하고 멤버 객체 반환
     */
    @Override
    public GroupMember validateGroupMembers(Long groupId, Long userId) {
        log.debug("그룹 멤버 소속 검증 - groupId: {}, userId: {}", groupId, userId);
        return groupMemberRepository.findByGroupGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));
    }

    /**
     * 어떤 그룹에도 존재하지 않아야 통과
     */
    @Override
    public void validateUserNotInAnyGroup(Long userId) {
        log.debug("어떤 그룹에도 미소속 여부 검증 - userId: {}", userId);
        if (groupMemberRepository.existsByUserId(userId)) {
            log.warn("이미 그룹에 소속된 유저 - userId: {}", userId);
            throw new AlreadyJoinedException(userId);
        }
        log.debug("그룹 미소속 검증 통과 - userId: {}", userId);
    }
}
