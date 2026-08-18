package com.insighton.core.usecase;

import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.gateway.service.GatewayService;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.domain.groups.event.GroupDeletedEvent;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.domain.region.service.RegionService;
import com.insighton.core.domain.sensors.service.SensorService;
import com.insighton.core.domain.widgets.service.WidgetService;
import com.insighton.core.usecase.group.GroupDeleteUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupDeleteUseCaseTest {

    @Mock
    private GroupService groupService;

    @Mock
    private GatewayService gatewayService;

    @Mock
    private LocationService locationService;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private WidgetService widgetService;

    @Mock
    private SensorService sensorService;

    @Mock
    private GroupRegistrationService groupRegistrationService;

    @Mock
    private RegionService regionService;

    @InjectMocks
    private GroupDeleteUseCase deleteGroup;

    @Test
    @DisplayName("그룹 삭제 성공 - 슈퍼 매니저만 가능하며 연관 멤버 및 그룹 데이터 모두 삭제")
    void deleteGroup_success() {
        // given
        Long groupId = 1L;
        Location mockLocation = mock(Location.class);
        given(mockLocation.getLocationId()).willReturn(10L);
        given(locationService.getLocationListByGroupId(groupId)).willReturn(List.of(mockLocation));

        Dashboard mockDashboard = mock(Dashboard.class);
        given(mockDashboard.getDashboardId()).willReturn(100L);
        given(dashboardService.getDashboardEntity(10L)).willReturn(mockDashboard);
        GroupMember mockMember = mock(GroupMember.class);
        given(mockMember.isSuperManager()).willReturn(true);
        given(groupMemberService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

        // when
        deleteGroup.deleteGroup(1L, 1L, "testToken");

        // then
        verify(groupService, times(1)).validateInviteToken(1L, "testToken");
        verify(gatewayService, times(1)).deleteByGroupId(1L);
        verify(sensorService, times(1)).deleteAll(1L);
        verify(groupMemberService, times(1)).deleteGroupMemberAll(1L, 1L);
        verify(widgetService, times(1)).deleteAllWidget(100L);
        verify(dashboardService, times(1)).deleteDashboard(10L);
        verify(locationService, times(1)).deleteLocationAll(1L);
        verify(groupService, times(1)).deleteGroup(1L);

        verify(eventPublisher, times(1)).publishEvent(any(GroupDeletedEvent.class));
    }

    @Test
    @DisplayName("그룹 삭제 실패 - 슈퍼 매니저가 아닐 때")
    void deleteGroup_notSuperManager() {
        // given
        GroupMember mockMember = mock(GroupMember.class);
        given(mockMember.isSuperManager()).willReturn(false);
        given(mockMember.getGroupMemberId()).willReturn(10L);
        given(groupMemberService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

        String token = "token";

        // when & then
        assertThatThrownBy(() -> deleteGroup.deleteGroup(1L, 1L, token))
                .isInstanceOf(NoPermissionException.class);
    }

    @Test
    @DisplayName("Location 모두 삭제 성공 - 연결된 대시보드 및 위젯도 삭제")
    void deleteLocationAll_success() {
        // given
        Long groupId = 1L;
        Long userId = 1L;
        String token = "testToken";

        GroupMember mockMember = mock(GroupMember.class);
        given(mockMember.isSuperManager()).willReturn(true);
        given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockMember);

        Location mockLocation = mock(Location.class);
        given(mockLocation.getLocationId()).willReturn(10L);
        given(locationService.getLocationListByGroupId(groupId)).willReturn(List.of(mockLocation));

        Dashboard mockDashboard = mock(Dashboard.class);
        given(mockDashboard.getDashboardId()).willReturn(100L);
        given(dashboardService.getDashboardEntity(10L)).willReturn(mockDashboard);

        // when
        deleteGroup.deleteGroup(userId, groupId, token);

        // then
        verify(widgetService, times(1)).deleteAllWidget(100L);
        verify(dashboardService, times(1)).deleteDashboard(10L);
        verify(locationService, times(1)).deleteLocationAll(groupId);
    }

}