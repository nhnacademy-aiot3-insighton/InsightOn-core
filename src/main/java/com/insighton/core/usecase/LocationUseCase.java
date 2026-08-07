package com.insighton.core.usecase;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.dashboards.dto.request.DashboardRequest;
import com.insighton.core.dashboards.entity.Dashboard;
import com.insighton.core.dashboards.service.DashboardService;
import com.insighton.core.groupmember.entity.GroupMember;
import com.insighton.core.groupmember.service.GroupMemberService;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.location.dto.request.LocationCreateRequest;
import com.insighton.core.location.dto.request.LocationUpdateRequest;
import com.insighton.core.location.dto.response.LocationListResponse;
import com.insighton.core.location.dto.response.LocationResponse;
import com.insighton.core.location.entity.Location;
import com.insighton.core.location.event.LocationDeletedEvent;
import com.insighton.core.location.service.LocationService;
import com.insighton.core.sensors.entity.Sensor;
import com.insighton.core.sensors.service.SensorService;
import com.insighton.core.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class LocationUseCase {
    private final GroupMemberService groupMemberService;
    private final LocationService locationService;
    private final ApplicationEventPublisher eventPublisher;
    private final DashboardService dashboardService;
    private final SensorService sensorService;
    private final WidgetService widgetService;

    /**
     * location 생성
     *
     * @param userId  location을 생성하려는 user의 ID
     * @param groupId location을 만드려는 group ID
     * @param request location 생성 request
     */
    @Transactional
    public void createLocation(Long userId, Long groupId, LocationCreateRequest request) {
        // 그룹이 존재한다면 그 그룹 안에 location을 만드려는 사람이 존재하는지 확인하고
        GroupMember groupMember = groupMemberService.validateGroupMembers(groupId, userId);

        // member가 member 권한일 때는 에러를 던지고
        validationIsAdmin(groupMember);

        // 만들기
        Location location = locationService.createLocation(groupMember.getGroup(), request);

        DashboardRequest dashboardRequest = new DashboardRequest(location.getLocationId(), request.locationName() + " - dashboard");

        dashboardService.createDashboard(location, dashboardRequest);
    }

    /**
     * location list 조회
     *
     * @param userId  list를 조회하는 user의 ID
     * @param groupId location들이 속해있는 group의 ID
     * @return location List 반환
     */
    @Transactional(readOnly = true)
    public List<LocationListResponse> getLocationList(Long userId, Long groupId) {
        // 그룹에 user가 존재하는지 확인
        groupMemberService.validateGroupMembers(groupId, userId);

        return locationService.getLocationList(groupId);
    }

    /**
     * location 상세 정보 조회
     *
     * @param userId  location의 상세 정보를 조회하려는 User의 ID
     * @param groupId location이 속해있는 group의 ID
     * @return location 상세 정보 반환
     */
    @Transactional(readOnly = true)
    public LocationResponse getLocation(Long userId, Long groupId, Long locationId) {
        // 그룹에 user가 존재하는지 확인
        groupMemberService.validateGroupMembers(groupId, userId);

        return locationService.getLocation(locationId, groupId);
    }

    /**
     * location mode 수정
     *
     * @param userId     정보를 수정하려는 user의 ID
     * @param groupId    수정하려는 location이 속해있는 group의 ID
     * @param locationId 수정하려는 location의 ID
     *                   user의 권한 확인 후 수정
     */
    @Transactional
    public void toggleAutoControlMode(Long userId, Long groupId, Long locationId) {

        GroupMember groupMember = groupMemberService.validateGroupMembers(groupId, userId);

        // member가 member 권한일 때는 에러를 던지고
        validationIsAdmin(groupMember);

        locationService.toggleAutoControlMode(locationId, groupId);
    }

    /**
     * location name 수정
     *
     * @param userId           정보를 수정하려는 user의 ID
     * @param groupId          수정하려는 location이 속해있는 group의 ID
     * @param targetLocationId 수정하려는 location의 ID
     * @param request          수정 할 이름
     */
    @Transactional
    public void updateName(Long userId, Long groupId, Long targetLocationId, LocationUpdateRequest request) {

        GroupMember groupMember = groupMemberService.validateGroupMembers(groupId, userId);

        // member가 member 권한일 때는 에러를 던지고
        validationIsAdmin(groupMember);

        locationService.updateName(targetLocationId, groupId, request);

        Location location = locationService.getLocationByGroupId(targetLocationId, groupId);

        // 이름이 바뀐 location entity 객체를 가져와서 dashboards title에도 적용시켜준다.
        DashboardRequest dashboardRequest = new DashboardRequest(location.getLocationId(), location.getLocationName() + " - dashboard");

        dashboardService.updateDashboardTitle(dashboardRequest);
    }

    /**
     * location 삭제
     * 하위에 있는 것들을 먼저 차근차근 삭제하시오 여기서.
     * 센서 센서는 삭제 말고 locations_id를 null 값으로 바꿔주셔?
     *
     * @param userId           삭제하려는 user의 ID
     * @param groupId          삭제될 location이 속해있는 group ID
     * @param targetLocationId 삭제될 location ID
     *                         user의 권한 확인 후 삭제
     */
    @Transactional
    public void deleteLocation(Long userId, Long groupId, Long targetLocationId) {
        // 그룹이 존재한다면 그 그룹 안에 location을 삭제하려는 사람이 존재하는지 확인하고
        GroupMember groupMember = groupMemberService.validateGroupMembers(groupId, userId);

        // member가 member 권한일 때는 에러를 던지고
        validationIsAdmin(groupMember);

        // sensors는 location 값만 null로 바꿔주기
        List<Sensor> sensors = sensorService.getSensorByLocationId(groupId, targetLocationId);
        for (Sensor sensor : sensors) {
            sensor.updateLocation(null);
        }

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

    /**
     * member가 관리자인지 확인
     */
    private void validationIsAdmin(GroupMember groupMember) {
        if (groupMember.isMember()) {
            throw NoPermissionException.forAdmin(groupMember.getGroupMemberId());
        }
    }
}
