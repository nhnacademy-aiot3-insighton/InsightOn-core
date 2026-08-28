package com.insighton.core.usecase.groupmember;

import com.insighton.core.adapter.client.internal.AuthClient;
import com.insighton.core.domain.groupmember.dto.response.AuthUserResponse;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.repository.GroupMemberRepository;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.service.GroupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GroupMemberInviteUserCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private GroupMemberRepository repository;

    @Mock
    private AuthClient authClient;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private GroupMemberInviteUserCase groupMemberInviteUserCase;

    private static final Long INVITER_ID = 1L;
    private static final Long INVITEE_ID = 2L;
    private static final Long GROUP_ID = 10L;
    private static final String TARGET_EMAIL = "testuser@insighton.io";

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("이메일로 그룹 멤버 초대 성공")
        void inviteMemberByEmail_success() {
            // given
            AuthUserResponse mockUser = new AuthUserResponse(INVITEE_ID, "테스트유저", "010-1234-5678", TARGET_EMAIL);
            Group mockGroup = Group.builder().name("테스트그룹").build();
            given(authClient.getUserResponseEmail(TARGET_EMAIL)).willReturn(mockUser);
            given(groupService.groupFindById(GROUP_ID)).willReturn(mockGroup);

            // when
            groupMemberInviteUserCase.inviteMemberByEmail(INVITER_ID, GROUP_ID, TARGET_EMAIL);

            // then
            verify(groupMemberService).validateGroupAdmin(GROUP_ID, INVITER_ID);
            verify(authClient).getUserResponseEmail(TARGET_EMAIL);
            verify(groupMemberService).validateUserNotInAnyGroup(INVITEE_ID);
            verify(repository).save(any(GroupMember.class));
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCases {

        @Test
        @DisplayName("가입되지 않은 이메일 주소일 경우 IllegalArgumentException 예외 발생")
        void inviteMemberByEmail_userNotFound() {
            // given
            given(authClient.getUserResponseEmail(TARGET_EMAIL)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> groupMemberInviteUserCase.inviteMemberByEmail(INVITER_ID, GROUP_ID, TARGET_EMAIL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가입되지 않은 이메일 주소 입니다.");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("초대한 사람에게 관리자 권한이 없을 경우 예외 발생")
        void inviteMemberByEmail_notAdmin() {
            // given
            willThrow(new IllegalArgumentException("관리자 권한이 없습니다."))
                    .given(groupMemberService).validateGroupAdmin(GROUP_ID, INVITER_ID);

            // when & then
            assertThatThrownBy(() -> groupMemberInviteUserCase.inviteMemberByEmail(INVITER_ID, GROUP_ID, TARGET_EMAIL))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(authClient, never()).getUserResponseEmail(any());
            verify(repository, never()).save(any());
        }
    }
}
