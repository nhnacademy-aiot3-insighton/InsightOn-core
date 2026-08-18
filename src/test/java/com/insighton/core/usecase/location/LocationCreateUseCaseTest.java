package com.insighton.core.usecase.location;

import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.entity.Group;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationCreateUseCaseTest {

    @Mock
    private LocationService locationService;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private LocationCreateUseCase managementUseCase;

    @Test
    @DisplayName("Location 생성 성공")
    void createLocation_success() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        LocationCreateRequest request = new LocationCreateRequest("test", Location.AutoControlMode.SUGGESTION);

        Group mockGroup = mock(Group.class);
        GroupMember mockGroupMember = mock(GroupMember.class);

        given(groupMemberService.validateGroupAdmin(groupId, userId)).willReturn(mockGroupMember);
        given(groupService.findWithLockByGroupId(groupId)).willReturn(mockGroup);

        Location mockLocation = mock(Location.class);
        given(mockLocation.getLocationId()).willReturn(1L);
        given(locationService.createLocation(mockGroup, request)).willReturn(mockLocation);

        // when
        managementUseCase.createLocation(userId, groupId, request);

        // then
        verify(groupMemberService, times(1)).validateGroupAdmin(groupId, userId);
        verify(locationService, times(1)).createLocation(mockGroup, request);
        verify(dashboardService, times(1)).createDashboard(any(), any());
    }

    @Test
    @DisplayName("Location 생성 실패 - 존재하지 않는 그룹인 경우 예외 발생")
    void createLocation_fail_groupNotFound() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        LocationCreateRequest request = new LocationCreateRequest("test", Location.AutoControlMode.SUGGESTION);

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        // when & then
        assertThatThrownBy(() -> managementUseCase.createLocation(userId, groupId, request))
                .isInstanceOf(GroupMemberNotFoundException.class);
    }

    @Test
    @DisplayName("Location 생성 실패 - 그룹에 속하지 않은 사용자인 경우 예외 발생")
    void createLocation_fail_memberNotFound() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        LocationCreateRequest request = new LocationCreateRequest("test", Location.AutoControlMode.SUGGESTION);

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        // when & then
        assertThatThrownBy(() -> managementUseCase.createLocation(userId, groupId, request))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verifyNoInteractions(locationService);
    }

    @Test
    @DisplayName("Location 생성 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
    void createLocation_fail_noPermission() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        LocationCreateRequest request = new LocationCreateRequest("test", Location.AutoControlMode.SUGGESTION);

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(10L));

        // when & then
        assertThatThrownBy(() -> managementUseCase.createLocation(userId, groupId, request))
                .isInstanceOf(NoPermissionException.class);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verifyNoInteractions(locationService);
    }

}