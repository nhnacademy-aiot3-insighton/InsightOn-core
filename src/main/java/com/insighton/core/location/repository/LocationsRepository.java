package com.insighton.core.location.repository;

import com.insighton.core.location.dto.response.LocationsListResponse;
import com.insighton.core.location.entity.Locations;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationsRepository extends JpaRepository<Locations, Long> {
    /**
     * groupID로 해당하는 location을 모두 조회
     *
     * @param groupsId 조회하고자 하는 location List의 group ID
     * @return location list
     */
    List<LocationsListResponse> findAllByGroups_GroupId(Long groupsId);

    /**
     * group ID와 LocationID 둘 다 해당하는 location의 상세 정보 조회
     *
     * @param locationId 상세 조회하고자 하는 location ID
     * @param groupId    조회하고자 하는 location의 group ID
     * @return location의 상세 정보
     */

    Optional<Locations> findByLocationIdAndGroups_GroupId(Long locationId, Long groupId);

    /**
     * group내에 같은 이름의 location이 존재하는지 조회
     *
     * @param groupId      group ID
     * @param locationName 조회하고자 하는 location의 ID
     * @return 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByGroups_GroupIdAndLocationName(Long groupId, String locationName);

    /**
     * groupID에 해당하는 모든 location 삭제
     *
     * @param groupId group ID
     */
    void deleteByGroups_GroupId(Long groupId);

    /**
     * group ID에 해당하는 location이 존재하는지 확인
     *
     * @param groupId group ID
     * @return 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByGroups_groupId(Long groupId);
}
