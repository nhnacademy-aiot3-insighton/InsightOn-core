package com.insighton.core.groupmember.service;

import com.insighton.core.adapter.client.internal.AuthClient;
import com.insighton.core.domain.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.domain.groupmember.dto.response.AuthUserResponse;
import com.insighton.core.domain.groupmember.dto.response.GroupMemberListResponse;
import com.insighton.core.domain.groupmember.dto.response.GroupMemberResponse;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.exception.AlreadyJoinedException;
import com.insighton.core.domain.groupmember.exception.SuperManagerCannotLeaveException;
import com.insighton.core.domain.groupmember.repository.GroupMemberRepository;
import com.insighton.core.domain.groupmember.service.impl.GroupMemberServiceImpl;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.groups.exception.UnAuthorizedAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupMemberServiceTest {

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private GroupMemberServiceImpl groupMemberService;

    // ==================== 가입 & 생성 ====================

    @Test
    @DisplayName("그룹 가입 성공")
    void joinGroupByToken_success() {
        // given
        Group group = mock(Group.class);
        GroupMemberJoinRequest request = new GroupMemberJoinRequest("testToken", 1L);
        given(groupMemberRepository.existsByUserId(1L)).willReturn(false);

        // when
        groupMemberService.joinGroupByToken(group, request);

        // then
        verify(groupMemberRepository, times(1)).save(any(GroupMember.class));
    }

    @Test
    @DisplayName("그룹 가입 실패 - 이미 가입된 유저")
    void joinGroupByToken_alreadyJoined() {
        // given
        Group group = mock(Group.class);
        GroupMemberJoinRequest request = new GroupMemberJoinRequest("testToken", 1L);
        given(groupMemberRepository.existsByUserId(1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> groupMemberService.joinGroupByToken(group, request))
                .isInstanceOf(AlreadyJoinedException.class);
    }

    @Test
    @DisplayName("슈퍼 매니저 생성 성공")
    void createGroupMember_success() {
        // given
        Group group = mock(Group.class);
        given(groupMemberRepository.existsByUserId(1L)).willReturn(false);

        // when
        groupMemberService.createGroupMember(group, 1L);

        // then
        verify(groupMemberRepository, times(1)).save(any(GroupMember.class));
    }

    // ==================== 조회 ====================

    @Test
    @DisplayName("그룹 멤버 목록 조회 성공")
    void getGroupMemberList_success() {
        // given
        GroupMember requester = mock(GroupMember.class);
        given(requester.isMember()).willReturn(false);
        given(groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L)).willReturn(Optional.of(requester));
        given(groupMemberRepository.findAllByGroupGroupId(1L)).willReturn(List.of(mock(GroupMemberListResponse.class)));

        // when
        List<GroupMemberListResponse> result = groupMemberService.getGroupMemberList(1L, 1L);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("그룹 멤버 목록 조회 실패 - 일반 멤버 권한")
    void getGroupMemberList_fail_unauthorized() {
        // given
        GroupMember requester = mock(GroupMember.class);
        given(requester.isMember()).willReturn(true);
        given(groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L)).willReturn(Optional.of(requester));

        // when & then
        assertThatThrownBy(() -> groupMemberService.getGroupMemberList(1L, 1L))
                .isInstanceOf(UnAuthorizedAccessException.class);
    }

    @Test
    @DisplayName("개별 멤버 조회 성공 - AuthClient 연동 포함")
    void getGroupMember_success() {
        // given
        Long userId = 2L;
        Long groupId = 1L;
        Long groupMemberId = 1L;

        GroupMember requester = mock(GroupMember.class);
        GroupMember target = mock(GroupMember.class);
        Group mockGroup = mock(Group.class); // 추가

        given(requester.isMember()).willReturn(false);
        given(requester.getUserId()).willReturn(userId); // userId 설정

        given(target.getUserId()).willReturn(userId); // target과 userId 일치
        given(target.getGroup()).willReturn(mockGroup); // 그룹 객체 연결
        given(mockGroup.getGroupId()).willReturn(groupId); // 그룹 ID 설정

        given(groupMemberRepository.findByGroupGroupIdAndUserId(groupId, userId)).willReturn(Optional.of(requester));
        given(groupMemberRepository.findByGroupMemberIdAndGroupGroupId(groupMemberId, groupId)).willReturn(Optional.of(target));
        given(authClient.getUserResponse(userId)).willReturn(new AuthUserResponse(userId, "testUser", "010-0000-0000", "녀어렁"));

        // when
        GroupMemberResponse result = groupMemberService.getGroupMember(userId, groupId, groupMemberId);

        // then
        assertThat(result.userName()).isEqualTo("testUser");
        assertThat(result.userId()).isEqualTo(userId);
    }

    // ==================== 권한 수정 & 강퇴 ====================

    @Test
    @DisplayName("관리자 역할 토글 성공 - MEMBER를 MANAGER로 승격 (SUPER_MANAGER가 요청)")
    void toggleManagerRole_promote_bySuperManager_success() {
        // given
        GroupMember admin = mock(GroupMember.class);
        GroupMember target = mock(GroupMember.class);
        given(target.isMember()).willReturn(true);
        given(target.getGroupRole()).willReturn(GroupMember.GroupRole.MEMBER);
        given(groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L)).willReturn(Optional.of(admin));
        given(groupMemberRepository.findByGroupMemberIdAndGroupGroupId(1L, 1L)).willReturn(Optional.of(target));

        // when
        groupMemberService.toggleManagerRole(1L, 1L, 1L);

        // then
        verify(target, times(1)).updateRole(GroupMember.GroupRole.MANAGER);
    }

    @Test
    @DisplayName("관리자 역할 토글 성공 - MANAGER를 MEMBER로 강등 (SUPER_MANAGER만 가능)")
    void toggleManagerRole_demote_bySuperManager_success() {
        // given
        GroupMember superAdmin = mock(GroupMember.class);
        GroupMember targetManager = mock(GroupMember.class);

        given(targetManager.isMember()).willReturn(false);
        given(targetManager.isManager()).willReturn(true);
        given(superAdmin.isMember()).willReturn(false);
        given(superAdmin.isManager()).willReturn(false);
        given(superAdmin.isSuperManager()).willReturn(true);
        given(targetManager.getGroupRole()).willReturn(GroupMember.GroupRole.MANAGER);

        given(groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L)).willReturn(Optional.of(superAdmin));
        given(groupMemberRepository.findByGroupMemberIdAndGroupGroupId(2L, 1L)).willReturn(Optional.of(targetManager));

        // when
        groupMemberService.toggleManagerRole(1L, 2L, 1L);

        // then
        verify(targetManager, times(1)).updateRole(GroupMember.GroupRole.MEMBER);
    }

    @Test
    @DisplayName("관리자 역할 토글 실패 - target이 MEMBER일 때 요청자가 일반 MEMBER이면 거부")
    void toggleManagerRole_fail_memberCannotChangeMember() {
        // given
        GroupMember admin = mock(GroupMember.class);
        GroupMember target = mock(GroupMember.class);
        given(target.isMember()).willReturn(true);
        given(admin.isMember()).willReturn(true);

        given(groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L)).willReturn(Optional.of(admin));
        given(groupMemberRepository.findByGroupMemberIdAndGroupGroupId(2L, 1L)).willReturn(Optional.of(target));

        // when & then
        assertThatThrownBy(() -> groupMemberService.toggleManagerRole(1L, 2L, 1L))
                .isInstanceOf(NoPermissionException.class);
    }

    @Test
    @DisplayName("관리자 역할 토글 실패 - target이 MANAGER일 때 요청자가 일반 MANAGER이면 거부")
    void toggleManagerRole_fail_managerCannotChangeManager() {
        // given
        GroupMember adminManager = mock(GroupMember.class);
        GroupMember targetManager = mock(GroupMember.class);

        given(targetManager.isMember()).willReturn(false);
        given(targetManager.isManager()).willReturn(true);
        given(adminManager.isMember()).willReturn(false);
        given(adminManager.isManager()).willReturn(true);

        given(groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L)).willReturn(Optional.of(adminManager));
        given(groupMemberRepository.findByGroupMemberIdAndGroupGroupId(2L, 1L)).willReturn(Optional.of(targetManager));

        // when & then
        assertThatThrownBy(() -> groupMemberService.toggleManagerRole(1L, 2L, 1L))
                .isInstanceOf(NoPermissionException.class);
    }

    @Test
    @DisplayName("관리자 역할 토글 실패 - target이 SUPER_MANAGER이면 누구든 변경 불가능")
    void toggleManagerRole_fail_cannotChangeSuperManager() {
        // given
        GroupMember admin = mock(GroupMember.class);
        GroupMember superManagerTarget = mock(GroupMember.class);

        given(superManagerTarget.isMember()).willReturn(false);
        given(superManagerTarget.isManager()).willReturn(false);
        given(superManagerTarget.isSuperManager()).willReturn(true);

        given(groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L)).willReturn(Optional.of(admin));
        given(groupMemberRepository.findByGroupMemberIdAndGroupGroupId(2L, 1L)).willReturn(Optional.of(superManagerTarget));

        // when & then
        assertThatThrownBy(() -> groupMemberService.toggleManagerRole(1L, 2L, 1L))
                .isInstanceOf(NoPermissionException.class);
    }

    @Test
    @DisplayName("멤버 강퇴 성공")
    void kickGroupMember_success() {
        // given
        GroupMember admin = mock(GroupMember.class);
        GroupMember target = mock(GroupMember.class);
        given(admin.isMember()).willReturn(false);
        given(admin.getUserId()).willReturn(1L);
        given(target.getUserId()).willReturn(2L);
        given(groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L)).willReturn(Optional.of(admin));
        given(groupMemberRepository.findByGroupMemberIdAndGroupGroupId(1L, 1L)).willReturn(Optional.of(target));

        // when
        groupMemberService.kickGroupMember(1L, 1L, 1L);

        // then
        verify(groupMemberRepository, times(1)).delete(target);
    }

    @Test
    @DisplayName("멤버 강퇴 실패 - 자기 자신을 강퇴하려 할 때")
    void kickGroupMember_fail_selfKick() {
        // given
        GroupMember admin = mock(GroupMember.class);
        GroupMember target = mock(GroupMember.class);
        given(admin.isMember()).willReturn(false);
        given(admin.getUserId()).willReturn(1L);
        given(target.getUserId()).willReturn(1L);
        given(groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L)).willReturn(Optional.of(admin));
        given(groupMemberRepository.findByGroupMemberIdAndGroupGroupId(1L, 1L)).willReturn(Optional.of(target));

        // when & then
        assertThatThrownBy(() -> groupMemberService.kickGroupMember(1L, 1L, 1L))
                .isInstanceOf(SuperManagerCannotLeaveException.class);
    }

    // ==================== 삭제 & 탈퇴 ====================

    @Test
    @DisplayName("그룹 삭제(멤버 전체 삭제) 성공")
    void deleteGroupMemberAll_success() {
        // given
        GroupMember admin = mock(GroupMember.class);
        given(admin.isSuperManager()).willReturn(true);
        given(groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L)).willReturn(Optional.of(admin));

        // when
        groupMemberService.deleteGroupMemberAll(1L, 1L);

        // then
        verify(groupMemberRepository, times(1)).deleteAllByGroupGroupId(1L);
    }

    @Test
    @DisplayName("그룹 탈퇴 성공")
    void leaveGroup_success() {
        // given
        GroupMember member = mock(GroupMember.class);
        given(member.isSuperManager()).willReturn(false);
        given(groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L)).willReturn(Optional.of(member));

        // when
        groupMemberService.leaveGroup(1L, 1L);

        // then
        verify(groupMemberRepository, times(1)).delete(member);
    }

    @Test
    @DisplayName("그룹 탈퇴 실패 - 슈퍼 매니저")
    void leaveGroup_fail_superManager() {
        // given
        GroupMember member = mock(GroupMember.class);
        given(member.isSuperManager()).willReturn(true);
        given(groupMemberRepository.findByGroupGroupIdAndUserId(1L, 1L)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> groupMemberService.leaveGroup(1L, 1L))
                .isInstanceOf(SuperManagerCannotLeaveException.class);
    }

    // ==================== 헬퍼 메서드 검증 ====================

    @Test
    @DisplayName("어떤 그룹에도 가입되어 있지 않은 유저 검증 성공")
    void validateUserNotInAnyGroup_success() {
        // given
        given(groupMemberRepository.existsByUserId(1L)).willReturn(false);

        // when & then
        groupMemberService.validateUserNotInAnyGroup(1L);
    }

    @Test
    @DisplayName("어떤 그룹에도 가입되어 있지 않은 유저 검증 실패 - 이미 존재할 때")
    void validateUserNotInAnyGroup_fail_alreadyJoined() {
        // given
        given(groupMemberRepository.existsByUserId(1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> groupMemberService.validateUserNotInAnyGroup(1L))
                .isInstanceOf(AlreadyJoinedException.class);
    }
}