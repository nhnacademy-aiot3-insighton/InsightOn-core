package com.insighton.core.domain.dashboards.service.impl;

import com.insighton.core.domain.dashboards.dto.request.DashboardRequest;
import com.insighton.core.domain.dashboards.dto.response.DashboardResponse;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.exception.DashboardNotFoundException;
import com.insighton.core.domain.dashboards.repository.DashboardRepository;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.widgets.dto.response.WidgetsListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final DashboardRepository dashboardRepository;

    @Override
    @Transactional
    public void createDashboard(Location location, DashboardRequest request) {
        Dashboard dashboard = Dashboard.builder()
                .location(location).title(request.title()).build();

        dashboardRepository.save(dashboard);
        log.info("대시보드 생성 완료 - locationId: {}, title: {}", location.getLocationId(), request.title());
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long locationId, List<WidgetsListResponse> widgetsList) {
        Dashboard dashboard = dashboardRepository.findByLocationLocationId(locationId)
                .orElseThrow(() -> new DashboardNotFoundException(locationId));

        return DashboardResponse.builder()
                .dashboardId(dashboard.getDashboardId())
                .title(dashboard.getTitle())
                .widgetsList(widgetsList)
                .build();
    }

    @Override
    @Transactional
    public void updateDashboardTitle(DashboardRequest request) {
        Dashboard dashboard = getDashboardWithLockByLocationId(request.locationId());

        dashboard.updateTitle(request.title());
        log.info("대시보드 제목 수정 완료 - locationId: {}, newTitle: {}", request.locationId(), request.title());
    }

    @Override
    @Transactional
    public void deleteDashboard(Long locationId) {
        Dashboard dashboard = getDashboardWithLockByLocationId(locationId);

        dashboardRepository.delete(dashboard);
        log.info("대시보드 삭제 완료 - locationId: {}", locationId);
    }

    @Override
    @Transactional
    public Dashboard getDashboardEntity(Long locationId) {
        return dashboardRepository.findByLocationLocationId(locationId)
                .orElseThrow(() -> new DashboardNotFoundException(locationId));
    }

    @Override
    public Dashboard getDashboardWithLockByLocationId(Long locationId) {
        return dashboardRepository.findWithLockByLocationLocationId(locationId)
                .orElseThrow(() -> new DashboardNotFoundException(locationId));
    }
}
