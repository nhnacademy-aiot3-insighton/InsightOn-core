package com.insighton.core.usecase.location;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.dashboards.dto.request.DashboardRequest;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.domain.location.dto.request.LocationCreateRequest;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class LocationCreateUseCase {
    private final GroupMemberService groupMemberService;
    private final GroupService groupService;
    private final LocationService locationService;
    private final DashboardService dashboardService;

    /**
     * location 생성
     *
     * @param userId  location을 생성하려는 user의 ID
     * @param groupId location을 만드려는 group ID
     * @param request location 생성 request
     */
    @Transactional
    public void createLocation(Long userId, Long groupId, LocationCreateRequest request) {
        // 그룹이 존재하며 관리자 권한이 있는지 확인
        groupMemberService.validateGroupAdmin(groupId, userId);

        Group group = groupService.findWithLockByGroupId(groupId);

        // 만들기
        Location location = locationService.createLocation(group, request);

        DashboardRequest dashboardRequest = new DashboardRequest(location.getLocationId(), request.locationName() + " - dashboard");

        dashboardService.createDashboard(location, dashboardRequest);
    }
}
