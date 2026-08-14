package com.insighton.core.domain.dashboards.repository;

import com.insighton.core.domain.dashboards.entity.Dashboard;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface DashboardRepository extends JpaRepository<Dashboard, Long> {


    /**
     * 단순 조회용 (락 없음)
     *
     * @param locationId 찾고 싶은 dashboard가 존재하는 location ID
     * @return dashboard entity 반환
     */
    Optional<Dashboard> findByLocationLocationId(Long locationId);

    /**
     * dashboard 저장/수정 시 동시성 제어를 위한 비관적 락 조회
     *
     * @param locationId 찾고 싶은 dashboard가 존재하는 location ID
     * @return 비관적 락이 걸린 dashboard entity 반환
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Dashboard> findWithLockByLocationLocationId(Long locationId);
}
