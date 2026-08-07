package com.insighton.core.widgets.service;

import com.insighton.core.dashboards.entity.Dashboard;
import com.insighton.core.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.widgets.dto.request.WidgetSaveRequest;
import com.insighton.core.widgets.dto.response.WidgetsListResponse;

import java.util.List;

public interface WidgetService {
    /**
     * 위젯 생성
     *
     * @param dashboard 위젯을 생성하려는 dashboard
     * @param request   생성 요청 DTO
     */
    Long createWidget(Dashboard dashboard, WidgetSaveRequest request);

    /**
     * widget들 한눈에 보기
     *
     * @param dashboardId 보고 싶은 widget들의 dashboard ID
     * @return 위젯들 반환
     */
    List<WidgetsListResponse> getWidgetList(Long dashboardId);

    /**
     * 위젯 정보 수정
     *
     * @param dashboardId    정보를 수정하려는 widget이 속한 dashboard ID
     * @param targetWidgetId 수정하려는 widget의 ID
     * @param request        위젯 정보 수정 DTO
     */
    void updateWidget(Long dashboardId, Long targetWidgetId, WidgetSaveRequest request);

    /**
     * widget 삭제
     *
     * @param dashboardId    삭제될 widget이 속해있는 dashboard ID
     * @param targetWidgetId 삭제할 widget의 ID
     */
    void deleteWidget(Long dashboardId, Long targetWidgetId);

    /**
     * Controller에서 chart.js가 받아서 그려줄 influxDB 정보를 불러와 DTO로 반환
     *
     * @param widgetId 그리고 싶은 widget ID?
     * @return Chart.js에 보낼 DTO 반환
     */
    ChartDataResponse getWidgetChartData(Long widgetId);

    /**
     * 외부에서 호출할  캐시 일괄 삭제 메서드
     *
     * @param widgetIds 삭제될 widgets
     */
    void evictCacheForWidgetIds(List<Long> widgetIds);


    /**
     * dashboard 삭제할 때 dashboard에 해당하는 widget 전부 삭제
     *
     * @param dashboardId 삭제 될 dashboard ID
     */
    void deleteAllWidget(Long dashboardId);
}
