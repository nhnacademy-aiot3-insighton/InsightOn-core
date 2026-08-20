package com.insighton.core.usecase.location;

import com.insighton.core.domain.dashboards.dto.request.DashboardRequest;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.location.dto.request.LocationUpdateRequest;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.service.LocationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationNameUpdateUseCaseTest {

    @Mock
    private LocationService locationService;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private LocationNameUpdateUseCase locationNameUpdateUseCase;

    // ==================== 로케이션 이름 수정 ====================

    @Test
    @DisplayName("Location 이름 수정 성공 - 관리자 권한 확인 후 수정")
    void updateName_success() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        Long targetLocationId = 10L;
        LocationUpdateRequest request = new LocationUpdateRequest("새이름");

        GroupMember mockGroupMember = mock(GroupMember.class);
        given(groupMemberService.validateGroupAdmin(groupId, userId)).willReturn(mockGroupMember);

        Location mockLocation = mock(Location.class);
        given(mockLocation.getLocationId()).willReturn(targetLocationId);
        given(mockLocation.getLocationName()).willReturn("새이름");
        given(locationService.getLocationByGroupId(targetLocationId, groupId)).willReturn(mockLocation);

        // when
        locationNameUpdateUseCase.updateName(userId, groupId, targetLocationId, request);

        // then
        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verify(locationService).updateName(targetLocationId, groupId, request);
        verify(dashboardService).updateDashboardTitle(any(DashboardRequest.class));
    }

    @Test
    @DisplayName("Location 이름 수정 실패 - 그룹에 유저가 존재하지 않는 경우 예외 발생")
    void updateName_fail_memberNotFound() {
        // given
        Long userId = 999L;
        Long groupId = 1L;
        Long targetLocationId = 10L;
        LocationUpdateRequest request = new LocationUpdateRequest("새이름");

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        // when & then
        assertThatThrownBy(() -> locationNameUpdateUseCase.updateName(userId, groupId, targetLocationId, request))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verifyNoInteractions(locationService);
    }

    @Test
    @DisplayName("Location 이름 수정 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
    void updateName_fail_noPermission() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        Long targetLocationId = 10L;
        LocationUpdateRequest request = new LocationUpdateRequest("새이름");

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(10L));

        // when & then
        assertThatThrownBy(() -> locationNameUpdateUseCase.updateName(userId, groupId, targetLocationId, request))
                .isInstanceOf(NoPermissionException.class);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verifyNoInteractions(locationService);
    }

}