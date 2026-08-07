package com.insighton.core.groups.repository;

import com.insighton.core.groups.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    /**
     * 고유한 초대 토큰(inviteToken)과 group ID 기반으로 그룹 엔티티를 조회합니다.
     *
     * @param groupId     조회하고자 하는 group ID
     * @param inviteToken 조회하고자 하는 그룹의 고유 초대 토큰
     * @return 조회된 그룹 엔티티를 포함하는 Optional 객체 (존재하지 않는 경우 빈 Optional 반환)
     */
    Optional<Group> findByInviteTokenAndGroupId(String inviteToken, Long groupId);

    /**
     * 고유한 초대 토큰(inviteToken)을 기반으로 그룹 엔티티를 조회합니다.
     *
     * @param inviteToken 조회하고자 하는 그룹의 고유 초대 토큰
     * @return 조회된 그룹 엔티티를 포함하는 Optional 객체 (존재하지 않는 경우 빈 Optional 반환)
     */
    Optional<Group> findByInviteToken(String inviteToken);
}
