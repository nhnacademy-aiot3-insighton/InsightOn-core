package com.insighton.core.usecase.location;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.location.event.LocationDeletedEvent;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.domain.sensors.service.SensorService;
import com.insighton.core.domain.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class LocationDeleteUseCase {
    private final GroupMemberService groupMemberService;
    private final LocationService locationService;
    private final ApplicationEventPublisher eventPublisher;
    private final DashboardService dashboardService;
    private final SensorService sensorService;
    private final ActuatorService actuatorService;
    private final WidgetService widgetService;

    /**
     * location 삭제
     * 센서는 삭제하지 않고 location_id만 null로 detach, 액추에이터는 location_id가 NOT NULL FK라 실행로그와 함께 삭제
     *
     * @param userId           삭제하려는 user의 ID
     * @param groupId          삭제될 location이 속해있는 group ID
     * @param targetLocationId 삭제될 location ID
     *                         user의 권한 확인 후 삭제
     */
    @Transactional
    public void deleteLocation(Long userId, Long groupId, Long targetLocationId) {
        // 그룹이 존재하고 삭제하려는 사람에게 관리자 권한이 있는지 확인
        groupMemberService.validateGroupAdmin(groupId, userId);

        // sensors는 location 값만 null로 바꿔주기
        sensorService.detachLocationFromSensors(groupId, targetLocationId);

        // actuator service에 액추에이터로그 삭제 후 액추에이터 삭제 로직이 있음
        actuatorService.deleteAllByLocationId(targetLocationId);

        // dashboards 삭제
        dashboardDelete(targetLocationId);

        locationService.deleteLocation(targetLocationId, groupId);

        eventPublisher.publishEvent(new LocationDeletedEvent(targetLocationId));
    }

    /**
     * dashboard delete
     * < @Transactional 안 붙인 이유는 붙이면 이거 호출해서 이 CoreManageMentUseCase 클래스 내에서 노란줄 뜸
     *
     * @param locationId 삭제할 location ID
     */
    private void dashboardDelete(Long locationId) {
        Dashboard dashboard = dashboardService.getDashboardEntity(locationId);

        widgetService.deleteAllWidget(dashboard.getDashboardId());

        dashboardService.deleteDashboard(locationId);
    }
}
