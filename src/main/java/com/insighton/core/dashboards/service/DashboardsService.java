package com.insighton.core.dashboards.service;

import com.insighton.core.dashboards.dto.request.DashboardsRequest;
import com.insighton.core.dashboards.dto.response.DashboardResponse;
import com.insighton.core.location.entity.Locations;

public interface DashboardsService {

    /**
     * location 생성에서 호출할 dashboard 생성
     *
     * @param locations dashboard가 생성될 location
     * @param request   dashboard 생성 요청 DTO
     */
    void createDashboard(Locations locations, DashboardsRequest request);

    /**
     * dashboards 조회
     *
     * @param locationId 조회할 dashboards가 속해있는 location ID
     * @return 조회하고자 하는 dashboard의 응답 DTO
     */
    DashboardResponse getDashboard(Long locationId);

    /**
     * dashboards title update
     *
     * @param request update 하고자 하는 정보
     */
    void updateDashboardTitle(DashboardsRequest request);

    /**
     * dashboards 삭제(dashboard만 따로 삭제할 수는 없고 location을 삭제할 때만 같이 삭제됨)
     *
     * @param locationId 삭제될 Location ID
     */
    void deleteDashboard(Long locationId);

}
