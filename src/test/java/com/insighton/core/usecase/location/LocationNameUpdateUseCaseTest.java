package com.insighton.core.usecase.location;

import com.insighton.core.domain.dashboards.dto.request.DashboardRequest;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.location.dto.request.LocationUpdateRequest;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
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
class LocationNameUpdateUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private LocationService locationService;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private LocationNameUpdateUseCase locationNameUpdateUseCase;

    @Test
    @DisplayName("location 이름 수정 성공 - 이름 변경 후 대시보드 제목도 함께 갱신")
    void updateName_success() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        Long targetLocationId = 10L;
        LocationUpdateRequest request = LocationUpdateRequest.builder()
                .newLocationName("3층 회의실")
                .build();

        Location mockLocation = mock(Location.class);
        given(mockLocation.getLocationId()).willReturn(targetLocationId);
        given(mockLocation.getLocationName()).willReturn("3층 회의실");
        given(locationService.getLocationByGroupId(targetLocationId, groupId)).willReturn(mockLocation);

        // when
        locationNameUpdateUseCase.updateName(userId, groupId, targetLocationId, request);

        // then
        verify(groupMemberService, times(1)).validateGroupAdmin(groupId, userId);
        verify(locationService, times(1)).updateName(targetLocationId, groupId, request);
        verify(locationService, times(1)).getLocationByGroupId(targetLocationId, groupId);
        verify(dashboardService, times(1))
                .updateDashboardTitle(new DashboardRequest(targetLocationId, "3층 회의실 - dashboard"));
    }

    @Test
    @DisplayName("location 이름 수정 실패 - 관리자 권한이 없는 경우 예외 발생")
    void updateName_fail_noPermission() {
        // given
        Long userId = 999L;
        Long groupId = 1L;
        Long targetLocationId = 10L;
        LocationUpdateRequest request = LocationUpdateRequest.builder()
                .newLocationName("3층 회의실")
                .build();

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        // when & then
        assertThatThrownBy(() -> locationNameUpdateUseCase.updateName(userId, groupId, targetLocationId, request))
                .isInstanceOf(NoPermissionException.class);

        verify(groupMemberService, times(1)).validateGroupAdmin(groupId, userId);
        verifyNoInteractions(locationService, dashboardService);
    }

    @Test
    @DisplayName("location 이름 수정 실패 - 이름 변경 후 location 재조회 시 존재하지 않는 경우 예외 발생")
    void updateName_fail_locationNotFound() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        Long targetLocationId = 999L;
        LocationUpdateRequest request = LocationUpdateRequest.builder()
                .newLocationName("3층 회의실")
                .build();

        given(locationService.getLocationByGroupId(targetLocationId, groupId))
                .willThrow(LocationNotFoundException.notFoundLocationByLocationId(targetLocationId));

        // when & then
        assertThatThrownBy(() -> locationNameUpdateUseCase.updateName(userId, groupId, targetLocationId, request))
                .isInstanceOf(LocationNotFoundException.class);

        verify(groupMemberService, times(1)).validateGroupAdmin(groupId, userId);
        verify(locationService, times(1)).updateName(targetLocationId, groupId, request);
        verify(locationService, times(1)).getLocationByGroupId(targetLocationId, groupId);
        verifyNoInteractions(dashboardService);
    }
}
