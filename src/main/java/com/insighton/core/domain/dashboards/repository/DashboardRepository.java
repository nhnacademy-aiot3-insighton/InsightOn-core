package com.insighton.core.domain.dashboards.repository;

import com.insighton.core.domain.dashboards.entity.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardRepository extends JpaRepository<Dashboard, Long> {


    /**
     * dashboard 수정하기 위해 entity를 조회하여 반환
     *
     * @param locationId 찾고 싶은 dashboard가 존재하는 location ID
     * @return dashboard entity 반환
     */
    Optional<Dashboard> findByLocationLocationId(Long locationId);
}
