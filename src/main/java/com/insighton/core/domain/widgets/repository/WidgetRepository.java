package com.insighton.core.domain.widgets.repository;

import com.insighton.core.domain.widgets.entity.Widget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WidgetRepository extends JpaRepository<Widget, Long> {

    /**
     * dashboard에 속한 모든 widget 조회
     *
     * @param dashboardId 조회하려는 dashboard ID
     * @return list 반환
     */
    List<Widget> findAllByDashboardDashboardId(Long dashboardId);

    /**
     * dashboard에 속한 widget 개별 조회
     *
     * @param widgetId    조회하고자 하는 widget ID
     * @param dashboardId 조회하고 싶은 widget이 속해있는 dashboard ID
     * @return widget entity 반환
     */
    Optional<Widget> findByWidgetIdAndDashboardDashboardId(Long widgetId, Long dashboardId);

    /**
     * dashboard에 속한 widget을 삭제
     *
     * @param widgetId    삭제하고자 하는 widget ID
     * @param dashboardId 삭제하고 싶은 widget이 속해있는 dashboard ID
     */
    void deleteByWidgetIdAndDashboardDashboardId(Long widgetId, Long dashboardId);

    /**
     * location이 삭제될 때 dashboard들도 삭제되니 모든 dashboard들의 아이디에 해당하는 widget을 삭제
     *
     * @param dashboardId 삭제될 dashboard ID
     */
    void deleteAllByDashboardDashboardId(Long dashboardId);


}
