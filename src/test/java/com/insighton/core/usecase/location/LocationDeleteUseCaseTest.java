package com.insighton.core.usecase.location;

import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.location.event.LocationDeletedEvent;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.domain.sensors.service.SensorService;
import com.insighton.core.domain.widgets.service.WidgetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LocationDeleteUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private LocationService locationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private SensorService sensorService;

    @Mock
    private ActuatorService actuatorService;

    @Mock
    private WidgetService widgetService;

    @InjectMocks
    private LocationDeleteUseCase locationDeleteUseCase;

    @Test
    @DisplayName("location 삭제 성공 - 센서는 detach, 액추에이터/대시보드/위젯은 삭제 후 location 삭제 및 이벤트 발행")
    void deleteLocation_success() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        Long targetLocationId = 10L;
        Long dashboardId = 100L;

        Dashboard mockDashboard = mock(Dashboard.class);
        given(mockDashboard.getDashboardId()).willReturn(dashboardId);
        given(dashboardService.getDashboardEntity(targetLocationId)).willReturn(mockDashboard);

        // when
        locationDeleteUseCase.deleteLocation(userId, groupId, targetLocationId);

        // then
        verify(groupMemberService, times(1)).validateGroupAdmin(groupId, userId);
        verify(sensorService, times(1)).detachLocationFromSensors(groupId, targetLocationId);
        verify(actuatorService, times(1)).deleteAllByLocationId(targetLocationId);
        verify(dashboardService, times(1)).getDashboardEntity(targetLocationId);
        verify(widgetService, times(1)).deleteAllWidget(dashboardId);
        verify(dashboardService, times(1)).deleteDashboard(targetLocationId);
        verify(locationService, times(1)).deleteLocation(targetLocationId, groupId);
        verify(eventPublisher, times(1)).publishEvent(eq(new LocationDeletedEvent(targetLocationId)));
    }

    @Test
    @DisplayName("location 삭제 실패 - 관리자 권한이 없는 경우 예외 발생")
    void deleteLocation_fail_noPermission() {
        // given
        Long userId = 999L;
        Long groupId = 1L;
        Long targetLocationId = 10L;

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        // when & then
        assertThatThrownBy(() -> locationDeleteUseCase.deleteLocation(userId, groupId, targetLocationId))
                .isInstanceOf(NoPermissionException.class);

        verify(groupMemberService, times(1)).validateGroupAdmin(groupId, userId);
        verifyNoInteractions(sensorService, actuatorService, dashboardService, locationService, widgetService, eventPublisher);
    }
}
