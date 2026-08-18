package com.insighton.core.usecase.location;

import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.location.service.LocationService;
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
class LocationModeUpdateUseCaseTest {
    @Mock
    private LocationService locationService;

    @Mock
    private GroupMemberService groupMemberService;

    @InjectMocks
    private LocationModeUpdateUseCase locationModeUpdateUseCase;

    // ==================== 로케이션 모드 수정 ====================

    @Test
    @DisplayName("Location 모드 수정 성공 - 관리자 권한 확인 후 변경")
    void toggleAutoControlMode_success() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        Long locationId = 10L;

        GroupMember mockGroupMember = mock(GroupMember.class);
        given(groupMemberService.validateGroupAdmin(groupId, userId)).willReturn(mockGroupMember);

        // when
        locationModeUpdateUseCase.toggleAutoControlMode(userId, groupId, locationId);

        // then
        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verify(locationService).toggleAutoControlMode(locationId, groupId);
    }

    @Test
    @DisplayName("Location 모드 수정 실패 - 그룹에 유저가 존재하지 않는 경우 예외 발생")
    void toggleAutoControlMode_fail_memberNotFound() {
        // given
        Long userId = 999L;
        Long groupId = 1L;
        Long locationId = 10L;

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        // when & then
        assertThatThrownBy(() -> locationModeUpdateUseCase.toggleAutoControlMode(userId, groupId, locationId))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verifyNoInteractions(locationService);
    }

    @Test
    @DisplayName("Location 모드 수정 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
    void toggleAutoControlMode_fail_noPermission() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        Long locationId = 10L;

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(10L));

        // when & then
        assertThatThrownBy(() -> locationModeUpdateUseCase.toggleAutoControlMode(userId, groupId, locationId))
                .isInstanceOf(NoPermissionException.class);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verifyNoInteractions(locationService);
    }

}