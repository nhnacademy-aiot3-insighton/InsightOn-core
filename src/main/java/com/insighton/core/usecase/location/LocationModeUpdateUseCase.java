package com.insighton.core.usecase.location;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class LocationModeUpdateUseCase {

    private final GroupMemberService groupMemberService;
    private final LocationService locationService;

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

        groupMemberService.validateGroupAdmin(groupId, userId);

        locationService.toggleAutoControlMode(locationId, groupId);
    }
}
