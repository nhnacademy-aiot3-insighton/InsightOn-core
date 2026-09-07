package com.insighton.core.usecase.location;

import com.insighton.core.domain.dashboards.dto.request.DashboardRequest;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.exception.GroupNotFoundException;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.domain.location.dto.request.LocationCreateRequest;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.service.LocationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LocationCreateUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private GroupService groupService;

    @Mock
    private LocationService locationService;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private LocationCreateUseCase locationCreateUseCase;

    @Test
    @DisplayName("location 생성 성공 - 관리자 권한 확인 후 location과 dashboard를 함께 생성")
    void createLocation_success() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        Long locationId = 10L;

        LocationCreateRequest request = LocationCreateRequest.builder()
                .locationName("4층 개발팀")
                .autoControlMode(Location.AutoControlMode.SUGGESTION)
                .build();

        Group mockGroup = mock(Group.class);
        given(groupService.findWithLockByGroupId(groupId)).willReturn(mockGroup);

        Location mockLocation = mock(Location.class);
        given(mockLocation.getLocationId()).willReturn(locationId);
        given(locationService.createLocation(mockGroup, request)).willReturn(mockLocation);

        // when
        locationCreateUseCase.createLocation(userId, groupId, request);

        // then
        verify(groupMemberService, times(1)).validateGroupAdmin(groupId, userId);
        verify(groupService, times(1)).findWithLockByGroupId(groupId);
        verify(locationService, times(1)).createLocation(mockGroup, request);
        verify(dashboardService, times(1))
                .createDashboard(mockLocation, new DashboardRequest(locationId, "4층 개발팀 - dashboard"));
    }

    @Test
    @DisplayName("location 생성 실패 - 관리자 권한이 없는 경우 예외 발생")
    void createLocation_fail_noPermission() {
        // given
        Long userId = 999L;
        Long groupId = 1L;

        LocationCreateRequest request = LocationCreateRequest.builder()
                .locationName("4층 개발팀")
                .autoControlMode(Location.AutoControlMode.SUGGESTION)
                .build();

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        // when & then
        assertThatThrownBy(() -> locationCreateUseCase.createLocation(userId, groupId, request))
                .isInstanceOf(NoPermissionException.class);

        verify(groupMemberService, times(1)).validateGroupAdmin(groupId, userId);
        verifyNoInteractions(groupService, locationService, dashboardService);
    }

    @Test
    @DisplayName("location 생성 실패 - 그룹이 존재하지 않는 경우 예외 발생")
    void createLocation_fail_groupNotFound() {
        // given
        Long userId = 100L;
        Long groupId = 999L;

        LocationCreateRequest request = LocationCreateRequest.builder()
                .locationName("4층 개발팀")
                .autoControlMode(Location.AutoControlMode.SUGGESTION)
                .build();

        given(groupService.findWithLockByGroupId(groupId))
                .willThrow(new GroupNotFoundException(groupId));

        // when & then
        assertThatThrownBy(() -> locationCreateUseCase.createLocation(userId, groupId, request))
                .isInstanceOf(GroupNotFoundException.class);

        verify(groupMemberService, times(1)).validateGroupAdmin(groupId, userId);
        verify(groupService, times(1)).findWithLockByGroupId(groupId);
        verifyNoInteractions(locationService, dashboardService);
    }
}
