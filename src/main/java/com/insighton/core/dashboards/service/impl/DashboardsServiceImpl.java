package com.insighton.core.dashboards.service.impl;

import com.insighton.core.dashboards.dto.request.DashboardsRequest;
import com.insighton.core.dashboards.dto.response.DashboardResponse;
import com.insighton.core.dashboards.entity.Dashboards;
import com.insighton.core.dashboards.exception.DashboardNotFoundExpception;
import com.insighton.core.dashboards.repository.DashboardsRepository;
import com.insighton.core.dashboards.service.DashboardsService;
import com.insighton.core.location.entity.Locations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardsServiceImpl implements DashboardsService {
    private final DashboardsRepository dashboardsRepository;

    @Override
    @Transactional
    public void createDashboard(Locations locations, DashboardsRequest request) {
        Dashboards dashboard = Dashboards.builder()
                .locations(locations).title(request.title()).build();

        dashboardsRepository.save(dashboard);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long locationId) {
        return dashboardsRepository.findProjectedByLocations_LocationId(locationId)
                .orElseThrow(() -> new DashboardNotFoundExpception(locationId));
    }

    @Override
    @Transactional
    public void updateDashboardTitle(DashboardsRequest request) {
        Dashboards dashboards = dashboardsRepository.findByLocations_LocationId(request.locationId())
                .orElseThrow(() -> new DashboardNotFoundExpception(request.locationId()));

        dashboards.updateTitle(request.title());
    }

    @Override
    @Transactional
    public void deleteDashboard(Long locationId) {
        Dashboards dashboards = dashboardsRepository.findByLocations_LocationId(locationId)
                .orElseThrow(() -> new DashboardNotFoundExpception(locationId));

        dashboardsRepository.delete(dashboards);
    }
}
