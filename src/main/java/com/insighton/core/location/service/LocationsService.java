package com.insighton.core.location.service;

import com.insighton.core.groups.entity.Groups;
import com.insighton.core.location.dto.request.LocationsCreateRequest;
import com.insighton.core.location.dto.request.LocationsUpdateRequest;
import com.insighton.core.location.dto.response.LocationsListResponse;
import com.insighton.core.location.dto.response.LocationsResponse;
import com.insighton.core.location.entity.Locations;

import java.util.List;

public interface LocationsService {
    /**
     * location 생성
     *
     * @param request location 생성 request
     */
    Locations createLocation(Groups groups, LocationsCreateRequest request);

    /**
     * location list 조회
     *
     * @param groupId location들이 속해있는 group의 ID
     * @return location List 반환
     */
    List<LocationsListResponse> getLocationList(Long groupId);

    /**
     * location 상세 정보 조회
     *
     * @param groupId location이 속해있는 group의 ID
     * @return location 상세 정보 반환
     */
    LocationsResponse getLocation(Long locationId, Long groupId);

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
    void updateName(Long locationId, Long groupId, LocationsUpdateRequest request);

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
     * dashboard 삭제할 때 가져올 location 정보
     *
     * @param groupId location이 속해있는 group의 ID
     * @return location entity 반환
     */
    Locations getLocationByGroupId(Long groupId);


}
