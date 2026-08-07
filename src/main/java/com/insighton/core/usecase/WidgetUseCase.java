package com.insighton.core.usecase;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.dashboards.entity.Dashboard;
import com.insighton.core.dashboards.service.DashboardService;
import com.insighton.core.groupmember.entity.GroupMember;
import com.insighton.core.groupmember.service.GroupMemberService;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.location.service.LocationService;
import com.insighton.core.widgets.dto.response.WidgetsListResponse;
import com.insighton.core.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class WidgetUseCase {
    private final GroupMemberService groupMemberService;
    private final LocationService locationService;
    private final DashboardService dashboardService;
    private final WidgetService widgetService;

    /**
     * widget list 조회
     *
     * @param userId     조회하려는 user ID
     * @param groupId    user가 속해있는 group ID
     * @param locationId widget을 조회하려는 dashboard가 속해있는 location ID
     * @return widget list 반환
     */
    @Transactional(readOnly = true)
    public List<WidgetsListResponse> getWidgetList(Long userId, Long groupId, Long locationId) {

        groupMemberService.validateGroupMembers(groupId, userId);

        locationService.getLocationByGroupId(locationId, groupId);

        Dashboard dashboard = dashboardService.getDashboardByLocationId(locationId);

        return widgetService.getWidgetList(dashboard.getDashboardId());
    }

    /**
     * widget 삭제
     */
    @Transactional
    public void deleteWidget(Long userId, Long groupId, Long locationId, Long targetWidgetId) {

        Dashboard dashboard = validateOnlyWidget(userId, groupId, locationId);

        widgetService.deleteWidget(dashboard.getDashboardId(), targetWidgetId);
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
