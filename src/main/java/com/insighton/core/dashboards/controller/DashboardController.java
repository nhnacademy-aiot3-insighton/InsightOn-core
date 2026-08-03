package com.insighton.core.dashboards.controller;

import com.insighton.core.dashboards.dto.response.DashboardResponse;
import com.insighton.core.groups.service.CoreManagementUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {
    private final CoreManagementUseCase useCase;

    /**
     * POST   /api/v1/groups/{group-id}/location/{location-id}/dashboard/widgets        위젯 추가
     * PUT    /api/v1/groups/{group-id}/location/{location-id}/dashboard/widgets/layout 레이아웃 일괄 저장
     * PUT    /api/v1/groups/{group-id}/location/{location-id}/dashboard/widgets/{widget-id}  위젯 개별 수정
     * DELETE /api/v1/groups/{group-id}/location/{location-id}/dashboard/widgets/{widget-id}  위젯 삭제
     * PUT    /api/v1/groups/{group-id}/location/{location-id}/dashboard/title          대시보드 제목 변경
     *
     * GET /api/v1/groups/{group-id}/location/{location-id}/dashboard
     * {
     *   "dashboardId": 1,
     *   "locationId": 42,
     *   "title": "4층 개발팀",
     *   "updatedAt": "2026-07-30T10:00:00+09:00",
     *   "widgets": [
     *     { "widgetId": 1, "type": "LINE_CHART", "metricKey": "co2",
     *       "xPos": 0, "yPos": 0, "width": 6, "height": 4 },
     *     { "widgetId": 2, "type": "GAUGE", "metricKey": "humidity",
     *       "xPos": 6, "yPos": 0, "width": 3, "height": 4 }
     *   ]
     * }
     */

    /**
     * dashboard 조회
     *
     * @param userId     조회하려는 user의 ID
     * @param groupId    조회하려는 dashboard가 속해있는 group의 ID
     * @param locationId 조회하려는 dashboard와 연결된 location ID
     * @return dashboard response 반환
     */
    @GetMapping("/api/v1/groups/{group-id}/locations/{location-id}/dashboard/")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("location-id") Long locationId
    ) {
        DashboardResponse response = useCase.getDashboard(userId, groupId, locationId);

        return ResponseEntity.ok(response);
    }
}
