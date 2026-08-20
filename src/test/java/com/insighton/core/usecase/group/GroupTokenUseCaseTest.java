package com.insighton.core.usecase.group;

import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.groups.service.GroupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class GroupTokenUseCaseTest {
    @Mock
    private GroupService groupService;

    @Mock
    private GroupMemberService groupMemberService;

    @InjectMocks
    private GroupTokenUseCase groupTokenUseCase;

    // ==================== 토큰 재발급 & 삭제 ====================

    @Test
    @DisplayName("토큰 재발급 성공 - 관리자 권한 확인")
    void newInviteToken_success() {
        // given
        GroupMember mockMember = mock(GroupMember.class);
        given(groupMemberService.validateGroupAdmin(1L, 1L)).willReturn(mockMember);

        // when
        groupTokenUseCase.newInviteToken(1L, 1L);

        // then
        verify(groupService, times(1)).newInviteToken(1L);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 일반 멤버 권한으로 시도 시")
    void newInviteToken_notAdmin() {
        // given
        given(groupMemberService.validateGroupAdmin(1L, 1L)).willThrow(NoPermissionException.forAdmin(10L));

        // when & then
        assertThatThrownBy(() -> groupTokenUseCase.newInviteToken(1L, 1L))
                .isInstanceOf(NoPermissionException.class);
    }

}