package com.insighton.core.groupmember.repository;

import com.insighton.core.groupmember.dto.response.GroupMembersListResponse;
import com.insighton.core.groupmember.entity.GroupMembers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMembersRepository extends JpaRepository<GroupMembers, Long> {


    /**
     * 존재하는 userID인지 체크하기 위한
     *
     * @param userId 조회하려는 user의 ID
     * @return 조회된 Group
     */
    boolean existsByUserId(Long userId);

    /**
     * 그룹 안에 user가 참여해 있는지 조회
     *
     * @param groupId user가 참여해있는 group의 ID
     * @param userId  조회하려는 user의 ID
     * @return group 참여 정보
     */
    Optional<GroupMembers> findByGroups_GroupIdAndUserId(Long groupId, Long userId);

    /**
     * group ID로 Group 내에 있는 GroupMemberList 조회
     *
     * @param groupId 조회하고자 하는 group의 ID
     * @return Group 내에 있는 GroupMemberList 반환
     */
    List<GroupMembersListResponse> findAllByGroups_GroupId(Long groupId);

    /**
     * 이미 그룹에 존재하는 user인지 조회
     *
     * @param groupId group ID
     * @param userId  존재 여부 확인할 user ID
     * @return 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByGroups_GroupIdAndUserId(Long groupId, Long userId);


    /**
     * user ID로 groupMember 조회
     *
     * @param userId 조회할 userID
     * @return user 아이디에 대한 groupMember 정보
     */
    Optional<GroupMembers> findByUserId(Long userId);

    /**
     * groupID로 찾은 모든 GroupMember들을 삭제
     *
     * @param groupId 삭제될 group ID
     */
    void deleteAllByGroups_GroupId(Long groupId);

    /**
     * 이 그룹에 이 그룹멤버 아이디를 가진 사람을 보는 게 맞는지(?)
     *
     * @param groupMemberId 조회하고자 하는 member의 groupMemberId
     * @param groupsId      조회하고자 하는 member가 속한 group의 ID
     * @return member 정보 반환
     */
    Optional<GroupMembers> findByGroupMemberIdAndGroups_GroupId(Long groupMemberId, Long groupsId);
}
