package com.insighton.core.usecase.location;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.dashboards.dto.request.DashboardRequest;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.location.dto.request.LocationUpdateRequest;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class LocationNameUpdateUseCase {
    private final GroupMemberService groupMemberService;
    private final LocationService locationService;
    private final DashboardService dashboardService;

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

        groupMemberService.validateGroupAdmin(groupId, userId);

        locationService.updateName(targetLocationId, groupId, request);

        Location location = locationService.getLocationByGroupId(targetLocationId, groupId);

        // 이름이 바뀐 location entity 객체를 가져와서 dashboards title에도 적용시켜준다.
        DashboardRequest dashboardRequest = new DashboardRequest(location.getLocationId(), location.getLocationName() + " - dashboard");

        dashboardService.updateDashboardTitle(dashboardRequest);
    }
}
