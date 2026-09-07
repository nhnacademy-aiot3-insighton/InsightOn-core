package com.insighton.core.usecase.chartdata;

import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.exception.DashboardNotFoundException;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.domain.widgets.dto.chart.ChartDataset;
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
class ChartDataUseCaseTest {

    @Mock
    private WidgetService widgetService;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private ChartDataUseCase chartDataUseCase;

    @Nested
    @DisplayName("위젯 차트 데이터 조회 케이스")
    class GetWidgetChartDataTest {

        @Test
        @DisplayName("차트 데이터 조회 성공 - 그룹 멤버 검증 후 대시보드/위젯을 거쳐 차트 데이터 반환")
        void getWidgetChartData_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 10L;
            Long widgetId = 20L;
            Long dashboardId = 50L;

            Dashboard mockDashboard = mock(Dashboard.class);
            given(mockDashboard.getDashboardId()).willReturn(dashboardId);


            ChartDataResponse expectedResponse = ChartDataResponse.builder()
                    .labels(List.of("10:00", "10:15"))
                    .datasets(List.of(new ChartDataset("temperature", List.of(23.5, 23.8))))
                    .build();

            given(dashboardService.getDashboardEntity(locationId)).willReturn(mockDashboard);
            given(widgetService.getWidgetChartData(dashboardId, widgetId)).willReturn(expectedResponse);

            // when
            ChartDataResponse response = chartDataUseCase.getWidgetChartData(userId, groupId, locationId, widgetId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.labels()).containsExactly("10:00", "10:15");
            assertThat(response.datasets()).hasSize(1);

            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
            verify(dashboardService, times(1)).getDashboardEntity(locationId);
            verify(widgetService, times(1)).getWidgetChartData(dashboardId, widgetId);
        }

        @Test
        @DisplayName("차트 데이터 조회 실패 - 그룹에 속하지 않은 사용자인 경우 예외 발생")
        void getWidgetChartData_fail_groupMemberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;
            Long locationId = 10L;
            Long widgetId = 20L;

            given(groupMemberService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> chartDataUseCase.getWidgetChartData(userId, groupId, locationId, widgetId))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
            verifyNoInteractions(dashboardService, widgetService);
        }

        @Test
        @DisplayName("차트 데이터 조회 실패 - 지정된 location에 대시보드가 존재하지 않는 경우 예외 발생")
        void getWidgetChartData_fail_dashboardNotFound() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 999L;
            Long widgetId = 20L;

            given(dashboardService.getDashboardEntity(locationId))
                    .willThrow(new DashboardNotFoundException(locationId));

            // when & then
            assertThatThrownBy(() -> chartDataUseCase.getWidgetChartData(userId, groupId, locationId, widgetId))
                    .isInstanceOf(DashboardNotFoundException.class);

            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
            verify(dashboardService, times(1)).getDashboardEntity(locationId);
            verifyNoInteractions(widgetService);
        }

//        @Test
//        @DisplayName("차트 데이터 조회 실패 - 대시보드에 해당 위젯이 존재하지 않는 경우 예외 발생")
//        void getWidgetChartData_fail_widgetNotFound() {
//            // given
//            Long userId = 100L;
//            Long groupId = 1L;
//            Long locationId = 10L;
//            Long widgetId = 999L;
//            Long dashboardId = 50L;
//
//            Dashboard mockDashboard = mock(Dashboard.class);
//            given(mockDashboard.getDashboardId()).willReturn(dashboardId);
//
//            given(dashboardService.getDashboardEntity(locationId)).willReturn(mockDashboard);
//            given(widgetService.getWidget(dashboardId, widgetId))
//                    .willThrow(WidgetNotFoundException.notFoundWidgetByWidgetId(widgetId));
//
//            // when & then
////            assertThatThrownBy(() -> chartDataUseCase.getWidgetChartData(userId, groupId, locationId, widgetId))
////                    .isInstanceOf(WidgetNotFoundException.class);
//
//            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
//            verify(dashboardService, times(1)).getDashboardEntity(locationId);
//            verify(widgetService, times(1)).getWidget(dashboardId, widgetId);
//            verify(widgetService, never()).getWidgetChartData(dashboardId, widgetId);
//        }
    }
}
