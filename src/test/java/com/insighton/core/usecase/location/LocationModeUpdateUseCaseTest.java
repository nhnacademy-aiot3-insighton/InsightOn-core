package com.insighton.core.usecase.location;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LocationModeUpdateUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationModeUpdateUseCase locationModeUpdateUseCase;

    @Test
    @DisplayName("자동 제어 모드 토글 성공 - 관리자 권한 확인 후 모드 토글")
    void toggleAutoControlMode_success() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        Long locationId = 10L;

        // when
        locationModeUpdateUseCase.toggleAutoControlMode(userId, groupId, locationId);

        // then
        verify(groupMemberService, times(1)).validateGroupAdmin(groupId, userId);
        verify(locationService, times(1)).toggleAutoControlMode(locationId, groupId);
    }

    @Test
    @DisplayName("자동 제어 모드 토글 실패 - 관리자 권한이 없는 경우 예외 발생")
    void toggleAutoControlMode_fail_noPermission() {
        // given
        Long userId = 999L;
        Long groupId = 1L;
        Long locationId = 10L;

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        // when & then
        assertThatThrownBy(() -> locationModeUpdateUseCase.toggleAutoControlMode(userId, groupId, locationId))
                .isInstanceOf(NoPermissionException.class);

        verify(groupMemberService, times(1)).validateGroupAdmin(groupId, userId);
        verifyNoInteractions(locationService);
    }
}
