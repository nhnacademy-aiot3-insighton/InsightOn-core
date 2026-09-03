package com.insighton.core.usecase.dashboard;

import com.insighton.core.domain.dashboards.dto.response.DashboardResponse;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.domain.widgets.dto.response.WidgetsListResponse;
import com.insighton.core.domain.widgets.service.WidgetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private LocationService locationService;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private WidgetService widgetService;

    @InjectMocks
    private DashboardUseCase dashboardUseCase;

    @Nested
    @DisplayName("Dashboard 조회 케이스")
    class GetDashboardTest {

        @Test
        @DisplayName("대시보드 조회 성공 - 그룹 멤버 및 location 검증 후 대시보드와 위젯 목록 반환")
        void getDashboard_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 10L;
            Long dashboardId = 50L;

            Dashboard mockDashboard = mock(Dashboard.class);
            given(mockDashboard.getDashboardId()).willReturn(dashboardId);

            WidgetsListResponse widget1 = mock(WidgetsListResponse.class);
            List<WidgetsListResponse> mockWidgetList = List.of(widget1);

            DashboardResponse expectedResponse = DashboardResponse.builder()
                    .dashboardId(dashboardId)
                    .title("거실 - dashboard")
                    .widgetsList(mockWidgetList)
                    .build();

            given(dashboardService.getDashboardEntity(locationId)).willReturn(mockDashboard);
            given(widgetService.getWidgetList(dashboardId)).willReturn(mockWidgetList);
            given(dashboardService.getDashboard(locationId, mockWidgetList)).willReturn(expectedResponse);

            // when
            DashboardResponse response = dashboardUseCase.getDashboard(userId, groupId, locationId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.dashboardId()).isEqualTo(dashboardId);
            assertThat(response.widgetsList()).hasSize(1);

            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
            verify(locationService, times(1)).getLocationByGroupId(locationId, groupId);
            verify(dashboardService, times(1)).getDashboardEntity(locationId);
            verify(widgetService, times(1)).getWidgetList(dashboardId);
            verify(dashboardService, times(1)).getDashboard(locationId, mockWidgetList);
        }

        @Test
        @DisplayName("대시보드 조회 실패 - 그룹에 속하지 않은 사용자인 경우 예외 발생")
        void getDashboard_fail_groupMemberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;
            Long locationId = 10L;

            given(groupMemberService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> dashboardUseCase.getDashboard(userId, groupId, locationId))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService, dashboardService, widgetService);
        }

        @Test
        @DisplayName("대시보드 조회 실패 - 지정된 location이 해당 그룹에 존재하지 않는 경우 예외 발생")
        void getDashboard_fail_locationNotFound() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 999L;

            given(locationService.getLocationByGroupId(locationId, groupId))
                    .willThrow(LocationNotFoundException.notFoundLocationByLocationId(locationId));

            // when & then
            assertThatThrownBy(() -> dashboardUseCase.getDashboard(userId, groupId, locationId))
                    .isInstanceOf(LocationNotFoundException.class);

            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
            verify(locationService, times(1)).getLocationByGroupId(locationId, groupId);
            verifyNoInteractions(dashboardService, widgetService);
        }
    }
}
