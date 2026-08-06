package com.insighton.core.widgets.repository;

import com.insighton.core.widgets.dto.response.WidgetsListResponse;
import com.insighton.core.widgets.entity.Widget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WidgetRepository extends JpaRepository<Widget, Long> {

    /**
     * dashboard에 속한 모든 widget 조회
     *
     * @param dashboardId 조회하려는 dashboard ID
     * @return list 반환
     */
    Optional<List<WidgetsListResponse>> findByDashboardDashboardsId(Long dashboardId);

    /**
     * dashboard에 속한 widget 개별 조회
     *
     * @param widgetId    조회하고자 하는 widget ID
     * @param dashboardId 조회하고 싶은 widget이 속해있는 dashboard ID
     * @return widget entity 반환
     */
    Optional<Widget> findByWidgetIdAndDashboardDashboardsId(Long widgetId, Long dashboardId);

    /**
     * dashboard에 속한 widget을 삭제
     *
     * @param widgetId    삭제하고자 하는 widget ID
     * @param dashboardId 삭제하고 싶은 widget이 속해있는 dashboard ID
     */
    void deleteByWidgetIdAndDashboardDashboardsId(Long widgetId, Long dashboardId);

    /**
     * location이 삭제될 때 dashboard들도 삭제되니 모든 dashboard들의 아이디에 해당하는 widget을 삭제
     *
     * @param dashboardId 삭제될 dashboard ID
     */
    void deleteAllByDashboardDashboardsId(Long dashboardId);

    /**
     * widget 위한 거
     */

    @Query("SELECT w.widgetId FROM Widget w WHERE w.dashboard.dashboardsId = :dashboardId")
    List<Long> findWidgetIdsByDashboardDashboardsId(@Param("dashboardId") Long dashboardId);

}
