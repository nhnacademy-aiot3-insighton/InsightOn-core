package com.insighton.core.dashboards.dto.response;

import com.insighton.core.widgets.dto.WidgetsListResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record DashboardResponse(
        Long dashboardId,
        List<WidgetsListResponse> widgetsList,
        String title) {
}
