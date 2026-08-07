package com.insighton.core.dashboards.controller;

import com.insighton.core.dashboards.dto.response.DashboardResponse;
import com.insighton.core.usecase.DashboardSaveUseCase;
import com.insighton.core.usecase.DashboardUseCase;
import com.insighton.core.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.widgets.dto.request.WidgetSaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups/{group-id}/location/{location-id}/dashboard")
public class DashboardController {
    private final DashboardUseCase dashboardUseCase;
    private final DashboardSaveUseCase dashboardSaveUseCase;

    /**
     * dashboard 조회
     *
     * @param userId     조회하려는 user의 ID
     * @param groupId    조회하려는 dashboard가 속해있는 group의 ID
     * @param locationId 조회하려는 dashboard와 연결된 location ID
     * @return dashboard response 반환
     */
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("location-id") Long locationId
    ) {
        DashboardResponse response = dashboardUseCase.getDashboard(userId, groupId, locationId);

        return ResponseEntity.ok(response);
    }

    /**
     * dashboard에 있는 저장 버튼을 눌렀을 때 반영되는 create와 update
     *
     * @param requests widget 생성과 수정 요청 dto
     * @return influxDB에서 가져온 정보 반환(id와 매칭해서)
     */
    @PostMapping("/save")
    public ResponseEntity<Map<Long, ChartDataResponse>> saveDashboard(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("location-id") Long locationId,
            @RequestBody List<WidgetSaveRequest> requests) {

        List<Long> widgetIds = dashboardSaveUseCase.saveDashboard(userId, groupId, locationId, requests);

        Map<Long, ChartDataResponse> result = dashboardSaveUseCase.saveDashboardInfluxDB(widgetIds);

        return ResponseEntity.ok(result);
    }
}
