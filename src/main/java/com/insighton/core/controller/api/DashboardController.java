package com.insighton.core.controller.api;

import com.insighton.core.controller.swagger.DashboardControllerApi;
import com.insighton.core.domain.dashboards.dto.response.DashboardResponse;
import com.insighton.core.domain.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.domain.widgets.dto.request.WidgetSaveRequest;
import com.insighton.core.usecase.dashboard.DashboardSaveUseCase;
import com.insighton.core.usecase.dashboard.DashboardUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups/{group-id}/location/{location-id}/dashboard")
public class DashboardController implements DashboardControllerApi {
    private final DashboardUseCase dashboardUseCase;
    private final DashboardSaveUseCase dashboardSaveUseCase;

    @Override
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("location-id") Long locationId
    ) {
        DashboardResponse response = dashboardUseCase.getDashboard(userId, groupId, locationId);

        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/save")
    public ResponseEntity<Map<Long, ChartDataResponse>> saveDashboard(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("location-id") Long locationId,
            @RequestBody List<@Valid WidgetSaveRequest> requests) {

        List<Long> widgetIds = dashboardSaveUseCase.saveDashboard(userId, groupId, locationId, requests);

        Map<Long, ChartDataResponse> result = dashboardSaveUseCase.saveDashboardInfluxDB(locationId, widgetIds);

        return ResponseEntity.ok(result);
    }
}
