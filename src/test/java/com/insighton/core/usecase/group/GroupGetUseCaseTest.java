package com.insighton.core.usecase.group;

import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.exception.AlreadyJoinedException;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.domain.groups.dto.response.GroupResponse;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.service.GroupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupGetUseCaseTest {

    @Mock
    private GroupService groupService;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private GroupRegistrationService groupRegistrationService;

    @InjectMocks
    private GroupGetUseCase managementUseCase;


    // ==================== 그룹 조회 (미리보기 & 내 그룹) ====================

    @Test
    @DisplayName("초대 그룹 프리뷰 조회 성공")
    void getGroupPreview_success() {
        // given
        given(groupService.getGroupPreview("token", 1L)).willReturn(mock(GroupResponse.class));

        // when
        GroupResponse response = managementUseCase.getGroupPreview("token", 1L, 1L);

        // then
        assertThat(response).isNotNull();
        verify(groupMemberService, times(1)).validateUserNotInAnyGroup(1L);
    }

    @Test
    @DisplayName("초대 그룹 프리뷰 조회 실패 - 이미 가입된 유저인 경우")
    void getGroupPreview_alreadyJoined() {
        // given
        willThrow(new AlreadyJoinedException(1L))
                .given(groupMemberService).validateUserNotInAnyGroup(1L);

        // when & then
        assertThatThrownBy(() -> managementUseCase.getGroupPreview("token", 1L, 1L))
                .isInstanceOf(AlreadyJoinedException.class);
    }

    @Test
    @DisplayName("내 그룹 정보 조회 성공 - 관리자 권한 응답 반환")
    void getMyGroup_success() {
        // given
        GroupMember mockMember = mock(GroupMember.class);
        Group mockGroup = mock(Group.class);
        given(mockMember.getGroup()).willReturn(mockGroup);
        given(mockMember.isManager()).willReturn(true);
        given(groupMemberService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

        // when
        GroupResponse response = managementUseCase.getMyGroup(1L, 1L);

        // then
        assertThat(response).isNotNull();
        verify(mockMember, times(1)).isManager();
    }

    @Test
    @DisplayName("내 그룹 정보 조회 실패 - 해당 그룹의 멤버가 아님")
    void getMyGroup_notFoundMember() {
        // given
        given(groupMemberService.validateGroupMembers(1L, 1L))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(1L, 1L));

        // when & then
        assertThatThrownBy(() -> managementUseCase.getMyGroup(1L, 1L))
                .isInstanceOf(GroupMemberNotFoundException.class);
    }
}