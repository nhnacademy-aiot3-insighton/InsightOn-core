package com.insighton.core.widgets.service;

import com.insighton.core.dashboards.entity.Dashboard;
import com.insighton.core.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.widgets.dto.request.WidgetCreateRequest;
import com.insighton.core.widgets.dto.request.WidgetLocationUpdateRequest;
import com.insighton.core.widgets.dto.request.WidgetUpdateRequest;
import com.insighton.core.widgets.dto.response.WidgetsListResponse;

import java.util.List;

public interface WidgetService {
    /**
     * 위젯 생성
     *
     * @param dashboard 위젯을 생성하려는 dashboard
     * @param request   생성 요청 DTO
     */
    void createWidget(Dashboard dashboard, WidgetCreateRequest request);

    /**
     * widget들 한눈에 보기
     *
     * @param dashboardId 보고 싶은 widget들의 dashboard ID
     * @return 위젯들 반환
     */
    List<WidgetsListResponse> getWidgetList(Long dashboardId);

    /**
     * 위젯 위치 변경
     *
     * @param dashboardId                 위치를 변경하고 싶은 widget이 존재하느 dashboard ID
     * @param widgetLocationUpdateRequest 위치 수정 요청 DTO
     */
    void updateLocationWidget(Long dashboardId, WidgetLocationUpdateRequest widgetLocationUpdateRequest);

    /**
     * 위젯 정보 수정
     *
     * @param dashboardId    정보를 수정하려는 widget이 속한 dashboard ID
     * @param targetWidgetId 수정하려는 widget의 ID
     * @param request        위젯 정보 수정 DTO
     */
    void updateWidget(Long dashboardId, Long targetWidgetId, WidgetUpdateRequest request);

    /**
     * widget 삭제
     *
     * @param dashboardId    삭제될 widget이 속해있는 dashboard ID
     * @param targetWidgetId 삭제할 widget의 ID
     */
    void deleteWidget(Long dashboardId, Long targetWidgetId);

    /**
     * dashboard가 삭제될 때(사실은 location delete) 속해있는 widget 전부를 삭제
     *
     * @param targetDashboardId 삭제될 dashboard ID
     */
    void deleteAllWidget(Long targetDashboardId);

    /**
     * Controller에서 chart.js가 받아서 그려줄 influxDB 정보를 불러와 DTO로 반환
     *
     * @param widgetId 그리고 싶은 widget ID?
     * @return Chart.js에 보낼 DTO 반환
     */
    ChartDataResponse getWidgetChartData(Long widgetId);
}
