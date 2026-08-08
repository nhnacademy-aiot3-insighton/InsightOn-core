package com.insighton.core.usecase;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.domain.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.domain.widgets.dto.request.WidgetSaveRequest;
import com.insighton.core.domain.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UseCase
@RequiredArgsConstructor
public class DashboardSaveUseCase {

    private final GroupMemberService groupMemberService;
    private final LocationService locationService;
    private final DashboardService dashboardService;
    private final WidgetService widgetService;

    /**
     * dashboard save 버튼 눌렀을 때 동작
     */
    @Transactional
    public List<Long> saveDashboard(Long userId, Long groupId, Long locationId, List<WidgetSaveRequest> requests) {
        Dashboard dashboard = validateOnlyWidget(userId, groupId, locationId);

        List<Long> widgetIds = new ArrayList<>();

        for (WidgetSaveRequest request : requests) {
            Long targetWidgetId;
            // Create : ID가 없다면 widget 생성 요청임
            if (request.widgetId() == null) {
                targetWidgetId = widgetService.createWidget(dashboard, request);
            } else {
                // Update : widget ID가 들어왔다면 기존 Widget 찾아서 수정
                targetWidgetId = request.widgetId();
                widgetService.updateWidget(dashboard.getDashboardId(), request.widgetId(), request);
            }
            widgetIds.add(targetWidgetId);
        }

        return widgetIds;
    }

    /**
     * 수정하거나 생성된 widget들의 config 값을 받아서 db method와 분리하여 influxDB 정보 불러오기
     */
    public Map<Long, ChartDataResponse> saveDashboardInfluxDB(List<Long> widgetIds) {

        Map<Long, ChartDataResponse> updatedChartDataMap = new HashMap<>();

        for (Long widgetId : widgetIds) {
            ChartDataResponse chartData = widgetService.getWidgetChartData(widgetId);
            updatedChartDataMap.put(widgetId, chartData);
        }

        return updatedChartDataMap;
    }

    /**
     * widget service 로직에서 반복되는 작업(권한 체크나 dashboard 가져오는 작업) 따로 분리
     */
    private Dashboard validateOnlyWidget(Long userId, Long groupId, Long locationId) {
        // user가 group에 속해있는지 확인하고
        GroupMember member = groupMemberService.validateGroupMembers(groupId, userId);

        // 속해있는 user가 관리자인지 확인하고
        validationIsAdmin(member);

        locationService.getLocationByGroupId(locationId, groupId);

        // locationID로 연결된 dashboard 가져와서
        return dashboardService.getDashboardByLocationId(locationId);
    }


    /**
     * member가 관리자인지 확인
     */
    private void validationIsAdmin(GroupMember groupMember) {
        if (groupMember.isMember()) {
            throw NoPermissionException.forAdmin(groupMember.getGroupMemberId());
        }
    }
}
