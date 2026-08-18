package com.insighton.core.usecase.location;

import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.domain.location.event.LocationDeletedEvent;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.domain.sensors.service.SensorService;
import com.insighton.core.domain.widgets.service.WidgetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationDeleteUseCaseTest {

    @Mock
    private LocationService locationService;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private SensorService sensorService;

    @Mock
    private WidgetService widgetService;

    @Mock
    private GroupService groupService;

    @Mock
    private ActuatorService actuatorService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LocationDeleteUseCase managementUseCase;

    @Nested
    @DisplayName("location test code")
    class LocationTest {

        // ==================== 로케이션 삭제 ====================

        @Test
        @DisplayName("Location 삭제 성공 - 관리자 권한 확인 후 삭제")
        void deleteLocation_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long targetLocationId = 10L;
            Long dashboardId = 100L;

            Dashboard mockDashboard = mock(Dashboard.class);
            given(mockDashboard.getDashboardId()).willReturn(dashboardId);
            given(dashboardService.getDashboardEntity(targetLocationId))
                    .willReturn(mockDashboard);

            GroupMember mockGroupMember = mock(GroupMember.class);
            given(groupMemberService.validateGroupAdmin(groupId, userId)).willReturn(mockGroupMember);

            // when
            managementUseCase.deleteLocation(userId, groupId, targetLocationId);

            // then
            verify(groupMemberService).validateGroupAdmin(groupId, userId);
            verify(sensorService).detachLocationFromSensors(groupId, targetLocationId);
            verify(actuatorService).deleteAllByLocationId(targetLocationId);
            verify(widgetService).deleteAllWidget(dashboardId);
            verify(dashboardService).deleteDashboard(targetLocationId);
            verify(locationService).deleteLocation(targetLocationId, groupId);
            verify(eventPublisher).publishEvent(any(LocationDeletedEvent.class));
        }

        @Test
        @DisplayName("Location 삭제 실패 - 삭제를 시도하는 사람이 그룹에 존재하지 않는 경우 예외 발생")
        void deleteLocation_fail_memberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;
            Long targetLocationId = 10L;

            given(groupMemberService.validateGroupAdmin(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> managementUseCase.deleteLocation(userId, groupId, targetLocationId))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMemberService).validateGroupAdmin(groupId, userId);
            verifyNoInteractions(locationService);
        }

        @Test
        @DisplayName("Location 삭제 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
        void deleteLocation_fail_noPermission() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long targetLocationId = 10L;

            given(groupMemberService.validateGroupAdmin(groupId, userId))
                    .willThrow(NoPermissionException.forAdmin(10L));

            // when & then
            assertThatThrownBy(() -> managementUseCase.deleteLocation(userId, groupId, targetLocationId))
                    .isInstanceOf(NoPermissionException.class);

            verify(groupMemberService).validateGroupAdmin(groupId, userId);
            verifyNoInteractions(locationService);
        }

    }
}

