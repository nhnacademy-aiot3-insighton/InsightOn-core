package com.insighton.core.usecase.chartdata;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.domain.widgets.entity.Widget;
import com.insighton.core.domain.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ChartDataUseCase {
    private final WidgetService widgetService;
    private final GroupMemberService groupMemberService;
    private final DashboardService dashboardService;

    @Transactional(readOnly = true)
    public ChartDataResponse getWidgetChartData(Long userId, Long groupId, Long locationId, Long widgetId) {
        // 1차로 user가 그룹에 속해있는지 확인
        groupMemberService.validateGroupMembers(groupId, userId);

        Dashboard dashboard = dashboardService.getDashboardEntity(locationId);

        Long dashboardId = dashboard.getDashboardId();

        Widget widget = widgetService.getWidget(dashboardId, widgetId);

        return widgetService.getWidgetChartData(dashboardId, widget.getWidgetId());
    }
}
