package com.insighton.core.location.service;

import com.insighton.core.groups.entity.Group;
import com.insighton.core.location.dto.request.LocationCreateRequest;
import com.insighton.core.location.dto.request.LocationUpdateRequest;
import com.insighton.core.location.dto.response.LocationListResponse;
import com.insighton.core.location.dto.response.LocationResponse;
import com.insighton.core.location.entity.Location;

import java.util.List;

public interface LocationService {
    /**
     * location 생성
     *
     * @param request location 생성 request
     */
    Location createLocation(Group group, LocationCreateRequest request);

    /**
     * location list 조회
     *
     * @param groupId location들이 속해있는 group의 ID
     * @return location List 반환
     */
    List<LocationListResponse> getLocationList(Long groupId);

    /**
     * location 상세 정보 조회
     *
     * @param groupId location이 속해있는 group의 ID
     * @return location 상세 정보 반환
     */
    LocationResponse getLocation(Long locationId, Long groupId);

    /**
     * mode 변경
     *
     * @param locationId 모드 변경 할 location ID
     */
    void toggleAutoControlMode(Long locationId, Long groupId);

    /**
     * location name 변경
     *
     * @param locationId 이름을 변경 할 Location ID
     * @param groupId    location이 속한 group ID
     * @param request    바꿀 이름 DTO...
     */
    void updateName(Long locationId, Long groupId, LocationUpdateRequest request);

    /**
     * location 삭제
     *
     * @param targetLocationId 삭제될 location ID
     */
    void deleteLocation(Long targetLocationId, Long groupId);

    /**
     * group 삭제 시 필요한 groupId로 찾은 location 전체 삭제
     *
     * @param groupId 삭제할 group ID
     */
    void deleteLocationAll(Long groupId);

    /**
     * group에 속해있는 location 조회
     *
     * @param groupId location이 속해있는 group의 ID
     * @return location entity 반환
     */
    Location getLocationByGroupId(Long locationId, Long groupId);

    /**
     * dashboard 삭제할 때 가져올 location 정보
     *
     * @param groupId location이 속해있는 group의 ID
     * @return location entity 반환
     */
    List<Location> getLocationListByGroupId(Long groupId);


}
