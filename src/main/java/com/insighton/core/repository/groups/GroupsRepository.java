package com.insighton.core.repository.groups;

import com.insighton.core.entity.groups.Groups;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupsRepository extends JpaRepository<Groups, Long> {

    /**
     * 고유한 초대 토큰(inviteToken)을 기반으로 그룹 엔티티를 조회합니다.
     *
     * @param inviteToken 조회하고자 하는 그룹의 고유 초대 토큰
     * @return 조회된 그룹 엔티티를 포함하는 Optional 객체 (존재하지 않는 경우 빈 Optional 반환)
     */
    Optional<Groups> findByInviteToken(String inviteToken);
}
