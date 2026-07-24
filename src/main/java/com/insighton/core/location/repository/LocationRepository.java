package com.insighton.core.location.repository;

import com.insighton.core.location.dto.response.LocationListResponse;
import com.insighton.core.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    /**
     * groupID로 해당하는 location을 모두 조회
     *
     * @param groupsId 조회하고자 하는 location List의 group ID
     * @return location list
     */
    List<LocationListResponse> findAllByGroups_GroupId(Long groupsId);

    /**
     * group ID와 LocationID 둘 다 해당하는 location의 상세 정보 조회
     *
     * @param locationId 상세 조회하고자 하는 location ID
     * @param groupId    조회하고자 하는 location의 group ID
     * @return location의 상세 정보
     */

    Optional<Location> findByLocationIdAndGroups_GroupId(Long locationId, Long groupId);

    /**
     * group내에 같은 이름의 location이 존재하는지 조회
     *
     * @param groupId      group ID
     * @param locationName 조회하고자 하는 location의 ID
     * @return 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByGroups_GroupIdAndLocationName(Long groupId, String locationName);
}
