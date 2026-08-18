package com.insighton.core.usecase.dashboard;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.domain.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.domain.widgets.dto.request.WidgetSaveRequest;
import com.insighton.core.domain.widgets.exception.AlreadyDashboardSaveException;
import com.insighton.core.domain.widgets.exception.WidgetNotFoundException;
import com.insighton.core.domain.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
        Long dashboardId = dashboard.getDashboardId();

        // 기존 DB에 존재하는 대시보드의 모든 위젯 ID 조회
        List<Long> existingWidgetIds = widgetService.getWidgetIdsByDashboardId(dashboardId);

        // 프론트 요청에서 넘어온 기존 위젯 ID List 추출
        List<Long> requestWidgetIds = requests.stream()
                .map(WidgetSaveRequest::widgetId)
                .filter(Objects::nonNull)
                .toList();

        for (Long existingId : existingWidgetIds) {
            // Delete : DB에는 있지만 front 요청 목록에는 없는 위젯 삭제
            if (!requestWidgetIds.contains(existingId)) {
                widgetService.deleteWidget(dashboardId, existingId);
            }
        }

        List<Long> widgetIds = new ArrayList<>();

        for (WidgetSaveRequest request : requests) {
            Long targetWidgetId;
            // Create : ID가 없다면 widget 생성 요청임
            if (request.widgetId() == null) {
                targetWidgetId = widgetService.createWidget(dashboard, request);
            } else {
                // Update : widget ID가 들어왔다면 기존 Widget 찾아서 수정
                targetWidgetId = request.widgetId();
                try {
                    widgetService.updateWidget(dashboardId, request.widgetId(), request);
                } catch (WidgetNotFoundException e) {
                    throw new AlreadyDashboardSaveException(dashboardId);
                }
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
        // user가 group에 속해있는지 및 관리자인지 확인
        groupMemberService.validateGroupAdmin(groupId, userId);

        locationService.getLocationByGroupId(locationId, groupId);

        // locationID로 연결된 dashboard 가져와서 (동시성 제어를 위해 비관적 락 적용)
        return dashboardService.getDashboardWithLockByLocationId(locationId);
    }
}
