package com.insighton.core.dashboards.repository;

import com.insighton.core.dashboards.dto.response.DashboardResponse;
import com.insighton.core.dashboards.entity.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardRepository extends JpaRepository<Dashboard, Long> {

    /**
     * location ID로 dashboard 조회
     * <p>
     * 프로젝션(Projection)을 사용하여 DB나 JPA에서 "전체 엔티티(테이블 모든 컬럼)를 다 안 가져오고,
     * 내가 필요한 특정 필드(DTO/인터페이스)만 쏙 뽑아서 조회하는 것으로 설정하여 메서드 이름 안 겹치게 함.
     *
     * @param locationId 조회할 dashboard의 location ID
     * @return dashboard response 반환
     */
    Optional<DashboardResponse> findProjectedByLocations_LocationId(Long locationId);

    /**
     * dashboard 수정하기 위해 entity를 조회하여 반환
     *
     * @param locationId 찾고 싶은 dashboard가 존재하는 location ID
     * @return dashboard entity 반환
     */
    Optional<Dashboard> findByLocations_LocationId(Long locationId);
}
