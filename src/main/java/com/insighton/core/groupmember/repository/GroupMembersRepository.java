package com.insighton.core.groupmember.repository;

import com.insighton.core.groupmember.entity.GroupMembers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupMembersRepository extends JpaRepository<GroupMembers, Long> {


    /**
     * 존재하는 userID인지 체크하기 위한
     * @param userId
     * @return groupcka
     */
    Optional<GroupMembers> findByUserId(Long userId);

    /**
     * 그룹 안에 user가 참여해 있는지 조회
     * @param groupId
     * @param userId
     * @return group 참여 정보
     */
    Optional<GroupMembers> findByGroups_GroupIdAndUserId(Long groupId, Long userId);

    /**
     * group ID로 Group이 존재하는지 조회
     * @param groupId 조회할 group ID
     * @return group의 정보
     */
    Optional<GroupMembers> findByGroups_GroupId(Long groupId);
}
