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
        log.debug("대시보드 생성 요청 - locationId: {}, title: {}", location.getLocationId(), request.title());
        Dashboard dashboard = Dashboard.builder()
                .location(location).title(request.title()).build();

        dashboardRepository.save(dashboard);
        log.info("대시보드 생성 완료 - locationId: {}, title: {}", location.getLocationId(), request.title());
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long locationId, List<WidgetsListResponse> widgetsList) {
        log.debug("대시보드 조회 요청 - locationId: {}", locationId);
        Dashboard dashboard = dashboardRepository.findByLocationLocationId(locationId)
                .orElseThrow(() -> new DashboardNotFoundException(locationId));

        log.info("대시보드 조회 완료 - locationId: {}, dashboardId: {}, title: {}", locationId, dashboard.getDashboardId(), dashboard.getTitle());
        return DashboardResponse.builder()
                .dashboardId(dashboard.getDashboardId())
                .title(dashboard.getTitle())
                .widgetsList(widgetsList)
                .build();
    }

    @Override
    @Transactional
    public void updateDashboardTitle(DashboardRequest request) {
        log.debug("대시보드 제목 수정 요청 - locationId: {}, title: {}", request.locationId(), request.title());
        Dashboard dashboard = getDashboardWithLockByLocationId(request.locationId());

        dashboard.updateTitle(request.title());
        log.info("대시보드 제목 수정 완료 - locationId: {}, newTitle: {}", request.locationId(), request.title());
    }

    @Override
    @Transactional
    public void deleteDashboard(Long locationId) {
        log.debug("대시보드 삭제 요청 - locationId: {}", locationId);
        Dashboard dashboard = getDashboardWithLockByLocationId(locationId);

        dashboardRepository.delete(dashboard);
        log.info("대시보드 삭제 완료 - locationId: {}", locationId);
    }

    @Override
    @Transactional
    public Dashboard getDashboardEntity(Long locationId) {
        log.debug("대시보드 엔티티 조회 - locationId: {}", locationId);
        return dashboardRepository.findByLocationLocationId(locationId)
                .orElseThrow(() -> new DashboardNotFoundException(locationId));
    }

    @Override
    public Dashboard getDashboardWithLockByLocationId(Long locationId) {
        log.debug("대시보드 비관적 락 조회 - locationId: {}", locationId);
        return dashboardRepository.findWithLockByLocationLocationId(locationId)
                .orElseThrow(() -> new DashboardNotFoundException(locationId));
    }
}
