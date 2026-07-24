package com.insighton.core.location.repository;

import com.insighton.core.location.dto.response.LocationListResponse;
import com.insighton.core.location.dto.response.LocationResponse;
import com.insighton.core.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    /**
     * groupID로 해당하는 location을 모두 조회
     * @param groupsId 조회하고자 하는 location List의 group ID
     * @return location list
     */
    List<LocationListResponse> findAllByGroups_GroupId(Long groupsId);

    /**
     * group ID와 LocationID 둘 다 해당하는 location의 상세 정보 조회
     * @param locationId 상세 조회하고자 하는 location ID
     * @param groupId 조회하고자 하는 location의 group ID
     * @return location의 상세 정보
     */
    Optional<LocationResponse> findByLocationIdAndGroups_GroupId(Long locationId, Long groupId);
}
