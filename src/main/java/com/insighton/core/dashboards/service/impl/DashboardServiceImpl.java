package com.insighton.core.dashboards.service.impl;

import com.insighton.core.dashboards.dto.request.DashboardRequest;
import com.insighton.core.dashboards.dto.response.DashboardResponse;
import com.insighton.core.dashboards.entity.Dashboard;
import com.insighton.core.dashboards.exception.DashboardNotFoundException;
import com.insighton.core.dashboards.repository.DashboardRepository;
import com.insighton.core.dashboards.service.DashboardService;
import com.insighton.core.location.entity.Location;
import com.insighton.core.widgets.dto.response.WidgetsListResponse;
import com.insighton.core.widgets.exception.WidgetNotFoundException;
import com.insighton.core.widgets.repository.WidgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final DashboardRepository dashboardRepository;
    private final WidgetRepository widgetRepository;

    @Override
    @Transactional
    public void createDashboard(Location location, DashboardRequest request) {
        Dashboard dashboard = Dashboard.builder()
                .location(location).title(request.title()).build();

        dashboardRepository.save(dashboard);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long locationId) {
        Dashboard dashboard = dashboardRepository.findByLocationLocationId(locationId)
                .orElseThrow(() -> new DashboardNotFoundException(locationId));

        List<WidgetsListResponse> widgetsList = widgetRepository.findByDashboardDashboardsId(dashboard.getDashboardsId())
                .orElseThrow(() -> WidgetNotFoundException.notFoundWidgetByDashboardId(dashboard.getDashboardsId()));

        return DashboardResponse.builder()
                .dashboardId(dashboard.getDashboardsId())
                .title(dashboard.getTitle())
                .widgetsList(widgetsList)
                .build();
    }

    @Override
    @Transactional
    public void updateDashboardTitle(DashboardRequest request) {
        Dashboard dashboard = dashboardRepository.findByLocationLocationId(request.locationId())
                .orElseThrow(() -> new DashboardNotFoundException(request.locationId()));

        dashboard.updateTitle(request.title());
    }

    @Override
    @Transactional
    public void deleteDashboard(Long locationId) {
        Dashboard dashboard = dashboardRepository.findByLocationLocationId(locationId)
                .orElseThrow(() -> new DashboardNotFoundException(locationId));
        // dashboard 삭제 전 해당 dashboard에 존재하는 widget들을 모두 삭제
        widgetRepository.deleteAllByDashboardDashboardsId(dashboard.getDashboardsId());

        dashboardRepository.delete(dashboard);
    }

    @Override
    public Dashboard getDashboardByLocationId(Long locationId) {
        
        return dashboardRepository.findByLocationLocationId(locationId)
                .orElseThrow(() -> new DashboardNotFoundException(locationId));
    }
}
