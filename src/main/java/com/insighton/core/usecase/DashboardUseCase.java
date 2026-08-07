package com.insighton.core.usecase;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.dashboards.dto.response.DashboardResponse;
import com.insighton.core.dashboards.entity.Dashboard;
import com.insighton.core.dashboards.service.DashboardService;
import com.insighton.core.groupmember.service.GroupMemberService;
import com.insighton.core.location.service.LocationService;
import com.insighton.core.widgets.dto.response.WidgetsListResponse;
import com.insighton.core.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class DashboardUseCase {
    
    private final GroupMemberService groupMemberService;
    private final LocationService locationService;
    private final DashboardService dashboardService;
    private final WidgetService widgetService;

    /**
     * dashboard 조회용 (갖고 있는 widget List까지 반환)
     *
     * @param userId     보려고 하는 user
     * @param groupId    속해있는 Group
     * @param locationId dashboard와 연결된 location
     * @return dashboard response 반환
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId, Long groupId, Long locationId) {
        // 조회하려는 user가 해당 그룹의 멤버인지 검증
        groupMemberService.validateGroupMembers(groupId, userId);

        // 조회하려는 dashboard가 해당 그룹의 location에 속해있는지 검증
        locationService.getLocationByGroupId(locationId, groupId);

        Dashboard dashboard = dashboardService.getDashboardEntity(locationId);

        List<WidgetsListResponse> widgetsList = widgetService.getWidgetList(dashboard.getDashboardId());

        return dashboardService.getDashboard(locationId, widgetsList);
    }
}
