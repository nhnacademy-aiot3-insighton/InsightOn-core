package com.insighton.core.usecase;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.gateway.service.GatewayService;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.domain.groups.event.GroupDeletedEvent;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.domain.region.service.RegionService;
import com.insighton.core.domain.sensors.service.SensorService;
import com.insighton.core.domain.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class GroupDeleteUseCase {
    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final LocationService locationService;
    private final GatewayService gatewayService;
    private final ApplicationEventPublisher eventPublisher;
    private final DashboardService dashboardService;
    private final WidgetService widgetService;
    private final SensorService sensorService;
    private final GroupRegistrationService groupRegistrationService;
    private final RegionService regionService;

    /**
     * 그룹 삭제
     * 초대 토큰을 입력 받아서 초대토큰이 맞다면 삭제(확인용)
     * 하위에서부터 차근차근 싹 다 삭제한 후에 group까지 delete하기
     * flow등은 삭제 요청 날리기 group이 삭제할거라고? 될거ㅏㄹ고?
     * <p>
     * ON DELETE CASCADE << 이거 사용해서 지우라고??
     *
     * @param userId  그룺을 삭제할 권한을 가진 userID
     * @param groupId 삭제될 group ID
     */
    @Transactional
    public void deleteGroup(Long userId, Long groupId, String inviteToken) {
        GroupMember groupMember = groupMemberService.validateGroupMembers(groupId, userId);

        if (!groupMember.isSuperManager()) {
            throw NoPermissionException.forAdmin(groupMember.getGroupMemberId());
        }

        groupService.findWithLockByGroupId(groupId);

        groupService.validateInviteToken(groupId, inviteToken);

        gatewayService.deleteByGroupId(groupId);

        sensorService.deleteAll(groupId);

        List<Long> locationIds = locationService.getLocationListByGroupId(groupId).stream()
                .map(Location::getLocationId)
                .toList();

        deleteLocationAll(groupId);

        groupMemberService.deleteGroupMemberAll(userId, groupId);

        groupService.deleteGroup(groupId);

        eventPublisher.publishEvent(new GroupDeletedEvent(groupId, locationIds));
    }

    /**
     * group이 삭제될 때 location도 전부 삭제
     *
     * @param groupId 삭제될 group ID
     */
    private void deleteLocationAll(Long groupId) {
        List<Location> locationList = locationService.getLocationListByGroupId(groupId);
        // dashboards 지우는 로직 추가
        for (Location location : locationList) {
            Long locationId = location.getLocationId();

            // dashboard 다 삭제해주기
            dashboardDelete(locationId);
        }
        // dashboards 삭제
        locationService.deleteLocationAll(groupId);
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
