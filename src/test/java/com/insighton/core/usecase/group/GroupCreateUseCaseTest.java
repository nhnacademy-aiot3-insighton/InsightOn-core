package com.insighton.core.usecase.group;

import com.insighton.core.domain.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.domain.groupmember.exception.AlreadyJoinedException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groupregistration.exception.AlreadyRequestedException;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.domain.groups.dto.request.GroupRequest;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.exception.InviteTokenNotFoundException;
import com.insighton.core.domain.groups.service.GroupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupCreateUseCaseTest {

    @Mock
    private GroupService groupService;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private GroupRegistrationService groupRegistrationService;

    @InjectMocks
    private GroupCreateUseCase managementUseCase;

    // ==================== 그룹 가입 ====================

    @Nested
    @DisplayName("Group test code")
    class GroupTest {
        @Test
        @DisplayName("그룹 가입 성공")
        void joinGroupByToken_success() {
            // given
            GroupMemberJoinRequest request = new GroupMemberJoinRequest("token", 1L);
            Group mockGroup = mock(Group.class);
            given(groupService.validateGroupByInviteToken("token")).willReturn(mockGroup);

            // when
            managementUseCase.joinGroupByToken(request);

            // then
            verify(groupMemberService, times(1)).joinGroupByToken(mockGroup, request);
        }

        @Test
        @DisplayName("그룹 가입 실패 - 존재하지 않는 초대 토큰")
        void joinGroupByToken_notFoundGroup() {
            // given
            GroupMemberJoinRequest request = new GroupMemberJoinRequest("bad-token", 1L);
            given(groupService.validateGroupByInviteToken("bad-token")).willThrow(InviteTokenNotFoundException.class);

            // when & then
            assertThatThrownBy(() -> managementUseCase.joinGroupByToken(request))
                    .isInstanceOf(InviteTokenNotFoundException.class);
        }

        @Test
        @DisplayName("그룹 가입 실패 - 이미 그룹에 가입된 유저")
        void joinGroupByToken_alreadyJoined() {
            // given
            GroupMemberJoinRequest request = new GroupMemberJoinRequest("token", 1L);
            Group mockGroup = mock(Group.class);
            given(groupService.validateGroupByInviteToken("token")).willReturn(mockGroup);
            willThrow(new AlreadyJoinedException(1L))
                    .given(groupMemberService).joinGroupByToken(mockGroup, request);

            // when & then
            assertThatThrownBy(() -> managementUseCase.joinGroupByToken(request))
                    .isInstanceOf(AlreadyJoinedException.class);
        }

        @Test
        @DisplayName("그룹 가입 실패 - 승인 대기 중인 신청서가 존재함")
        void joinGroupByToken_alreadyRequested() {
            // given
            GroupMemberJoinRequest request = new GroupMemberJoinRequest("token", 1L);
            Group mockGroup = mock(Group.class);
            given(groupService.validateGroupByInviteToken("token")).willReturn(mockGroup);
            willThrow(AlreadyRequestedException.of())
                    .given(groupRegistrationService).validateNoPendingRequest(1L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.joinGroupByToken(request))
                    .isInstanceOf(AlreadyRequestedException.class);
        }

        // ==================== 그룹 생성 ====================

        @Test
        @DisplayName("그룹 생성 성공")
        void createGroup_success() {
            // given
            GroupRequest request = new GroupRequest("name", "desc", "loc");
            Group mockGroup = mock(Group.class);
            given(groupService.createGroup(request)).willReturn(mockGroup);

            // when
            managementUseCase.createGroup(request, 1L);

            // then
            verify(groupMemberService, times(1)).createGroupMember(mockGroup, 1L);
        }

        @Test
        @DisplayName("그룹 생성 실패 - 이미 다른 그룹에 가입되어 있어 멤버 생성이 불가능할 때")
        void createGroup_alreadyJoined() {
            // given
            GroupRequest request = new GroupRequest("name", "desc", "loc");
            Group mockGroup = mock(Group.class);
            given(groupService.createGroup(request)).willReturn(mockGroup);
            willThrow(new AlreadyJoinedException(1L))
                    .given(groupMemberService).createGroupMember(mockGroup, 1L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.createGroup(request, 1L))
                    .isInstanceOf(AlreadyJoinedException.class);
        }
    }
}




