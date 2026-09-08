package com.insighton.core.usecase.location;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.location.dto.response.LocationListResponse;
import com.insighton.core.domain.location.dto.response.LocationResponse;
import com.insighton.core.domain.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class LocationGetUseCase {
    private final GroupMemberService groupMemberService;
    private final LocationService locationService;

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
}
