package com.insighton.core.dashboards.repository;

import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.repository.DashboardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = "/dashboard-test.sql")
class DashboardRepositoryTest {

    @Autowired
    private DashboardRepository dashboardRepository;

    @Test
    @DisplayName("Location ID로 대시보드 비관적 락 조회 성공 - SQL 기본 데이터 로드 확인")
    void findByLocationLocationId_success() {
        // given
        // 1L은 dashboard-test.sql에서 생성된 기본 locationId

        // when
        Optional<Dashboard> found = dashboardRepository.findByLocationLocationId(1L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("test-dashboard");
        assertThat(found.get().getLocation().getLocationId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 Location ID로 대시보드 조회 시 빈 Optional 반환")
    void findByLocationLocationId_notFound() {
        // when
        Optional<Dashboard> found = dashboardRepository.findByLocationLocationId(999L);

        // then
        assertThat(found).isEmpty();
    }
}

