package com.insighton.core.usecase;

import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.domain.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.domain.widgets.dto.request.WidgetSaveRequest;
import com.insighton.core.domain.widgets.exception.AlreadyDashboardSaveException;
import com.insighton.core.domain.widgets.exception.WidgetNotFoundException;
import com.insighton.core.domain.widgets.service.WidgetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardSaveUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private LocationService locationService;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private WidgetService widgetService;

    @InjectMocks
    private DashboardSaveUseCase dashboardSaveUseCase;

    @Nested
    @DisplayName("Dashboard 저장 케이스")
    class SaveDashboardTest {

        @Test
        @DisplayName("대시보드 저장 성공 - 신규 생성, 기존 수정, 미포함 위젯 삭제 처리")
        void saveDashboard_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 10L;
            Long dashboardId = 50L;

            GroupMember mockMember = mock(GroupMember.class);
            given(mockMember.isMember()).willReturn(false); // 관리자 권한
            given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockMember);

            Dashboard mockDashboard = mock(Dashboard.class);
            given(mockDashboard.getDashboardId()).willReturn(dashboardId);
            given(dashboardService.getDashboardWithLockByLocationId(locationId)).willReturn(mockDashboard);

            // DB에는 1번(수정대상), 2번(삭제대상) 위젯이 존재함
            given(widgetService.getWidgetIdsByDashboardId(dashboardId)).willReturn(List.of(1L, 2L));

            // 프론트 요청: 1번 위젯 수정 + null(신규 생성) 위젯
            WidgetSaveRequest updateRequest = WidgetSaveRequest.builder()
                    .widgetId(1L)
                    .xPos(0)
                    .yPos(0)
                    .width(6)
                    .height(4)
                    .build();

            WidgetSaveRequest createRequest = WidgetSaveRequest.builder()
                    .widgetId(null)
                    .xPos(6)
                    .yPos(0)
                    .width(6)
                    .height(4)
                    .build();

            given(widgetService.createWidget(mockDashboard, createRequest)).willReturn(3L);

            List<WidgetSaveRequest> requests = List.of(updateRequest, createRequest);

            // when
            List<Long> resultWidgetIds = dashboardSaveUseCase.saveDashboard(userId, groupId, locationId, requests);

            // then
            assertThat(resultWidgetIds).containsExactly(1L, 3L);

            // 2번 위젯 삭제 검증
            verify(widgetService, times(1)).deleteWidget(dashboardId, 2L);
            // 1번 위젯 수정 검증
            verify(widgetService, times(1)).updateWidget(dashboardId, 1L, updateRequest);
            // 신규 위젯 생성 검증
            verify(widgetService, times(1)).createWidget(mockDashboard, createRequest);
        }

        @Test
        @DisplayName("대시보드 저장 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
        void saveDashboard_fail_noPermission() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 10L;

            GroupMember mockMember = mock(GroupMember.class);
            given(mockMember.isMember()).willReturn(true); // 일반 멤버 권한
            given(mockMember.getGroupMemberId()).willReturn(200L);
            given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockMember);

            // when & then
            assertThatThrownBy(() -> dashboardSaveUseCase.saveDashboard(userId, groupId, locationId, List.of()))
                    .isInstanceOf(NoPermissionException.class);

            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService, dashboardService, widgetService);
        }

        @Test
        @DisplayName("대시보드 저장 실패 - 수정하려는 위젯이 존재하지 않는 경우 AlreadyDashboardSaveException 발생")
        void saveDashboard_fail_widgetNotFound() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 10L;
            Long dashboardId = 50L;
            Long targetWidgetId = 999L;

            GroupMember mockMember = mock(GroupMember.class);
            given(mockMember.isMember()).willReturn(false);
            given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockMember);

            Dashboard mockDashboard = mock(Dashboard.class);
            given(mockDashboard.getDashboardId()).willReturn(dashboardId);
            given(dashboardService.getDashboardWithLockByLocationId(locationId)).willReturn(mockDashboard);

            given(widgetService.getWidgetIdsByDashboardId(dashboardId)).willReturn(List.of(targetWidgetId));

            WidgetSaveRequest updateRequest = mock(WidgetSaveRequest.class);
            given(updateRequest.widgetId()).willReturn(targetWidgetId);

            willThrow(WidgetNotFoundException.notFoundWidgetByWidgetId(targetWidgetId))
                    .given(widgetService).updateWidget(eq(dashboardId), eq(targetWidgetId), any());

            List<WidgetSaveRequest> requests = List.of(updateRequest);

            // when & then
            assertThatThrownBy(() -> dashboardSaveUseCase.saveDashboard(userId, groupId, locationId, requests))
                    .isInstanceOf(AlreadyDashboardSaveException.class);
        }
    }

    @Nested
    @DisplayName("InfluxDB 차트 데이터 매핑 케이스")
    class SaveDashboardInfluxDBTest {

        @Test
        @DisplayName("위젯 ID 목록으로 차트 데이터 맵 구성 성공")
        void saveDashboardInfluxDB_success() {
            // given
            List<Long> widgetIds = List.of(1L, 2L);
            ChartDataResponse mockChartData1 = mock(ChartDataResponse.class);
            ChartDataResponse mockChartData2 = mock(ChartDataResponse.class);

            given(widgetService.getWidgetChartData(1L)).willReturn(mockChartData1);
            given(widgetService.getWidgetChartData(2L)).willReturn(mockChartData2);

            // when
            Map<Long, ChartDataResponse> result = dashboardSaveUseCase.saveDashboardInfluxDB(widgetIds);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(1L)).isEqualTo(mockChartData1);
            assertThat(result.get(2L)).isEqualTo(mockChartData2);

            verify(widgetService, times(1)).getWidgetChartData(1L);
            verify(widgetService, times(1)).getWidgetChartData(2L);
        }
    }
}
