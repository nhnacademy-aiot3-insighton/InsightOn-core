package com.insighton.core.domain.dashboards.service;

import com.insighton.core.domain.dashboards.dto.request.DashboardRequest;
import com.insighton.core.domain.dashboards.dto.response.DashboardResponse;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.widgets.dto.response.WidgetsListResponse;

import java.util.List;

public interface DashboardService {

    /**
     * location 생성에서 호출할 dashboard 생성
     *
     * @param location dashboard가 생성될 location
     * @param request  dashboard 생성 요청 DTO
     */
    void createDashboard(Location location, DashboardRequest request);

    /**
     * dashboards 조회
     *
     * @param locationId 조회할 dashboards가 속해있는 location ID
     * @return 조회하고자 하는 dashboard의 응답 DTO
     */
    DashboardResponse getDashboard(Long locationId, List<WidgetsListResponse> widgetsList);

    /**
     * dashboards title update
     *
     * @param request update 하고자 하는 정보
     */
    void updateDashboardTitle(DashboardRequest request);

    /**
     * dashboards 삭제(dashboard만 따로 삭제할 수는 없고 location을 삭제할 때만 같이 삭제됨)
     *
     * @param locationId 삭제될 Location ID
     */
    void deleteDashboard(Long locationId);

    /**
     * dashboard entity를 반환
     *
     * @param locationId dashboard와 연결된 location ID
     * @return dashboard entity
     */
    Dashboard getDashboardEntity(Long locationId);

    /**
     * 비관적 락을 적용하여 dashboard entity를 반환 (동시성 제어용)
     *
     * @param locationId dashboard와 연결된 location ID
     * @return 락이 걸린 dashboard entity
     */
    Dashboard getDashboardWithLockByLocationId(Long locationId);

}
