package com.insighton.core.location.service;

import com.insighton.core.groups.entity.Groups;
import com.insighton.core.location.dto.request.LocationCreateRequest;
import com.insighton.core.location.dto.response.LocationListResponse;
import com.insighton.core.location.dto.response.LocationResponse;

import java.util.List;

public interface LocationService {
    /**
     * location 생성
     *
     * @param request location 생성 request
     */
    void createLocation(Groups groups, LocationCreateRequest request);

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
    LocationResponse getLocation(Long groupId, Long locationId);

    /**
     * mode 변경
     *
     * @param locationId
     */
    void toggleAutoControlMode(Long locationId);

    /**
     * location 삭제
     *
     * @param targetLocationId 삭제될 location ID
     */
    void deleteLocation(Long targetLocationId);
}
