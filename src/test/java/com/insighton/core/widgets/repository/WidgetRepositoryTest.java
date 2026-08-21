package com.insighton.core.widgets.repository;

import com.insighton.core.domain.widgets.entity.Widget;
import com.insighton.core.domain.widgets.repository.WidgetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = "/widget-test.sql")
class WidgetRepositoryTest {

    @Autowired
    private WidgetRepository widgetRepository;

    @Test
    @DisplayName("대시보드 ID로 속한 모든 위젯 조회 성공")
    void findAllByDashboardDashboardId_success() {
        // when
        List<Widget> widgets = widgetRepository.findAllByDashboardDashboardId(1L);

        // then
        assertThat(widgets).hasSize(1);
        assertThat(widgets.getFirst().getDashboard().getDashboardId()).isEqualTo(1L);
        assertThat(widgets.getFirst().getXPos()).isZero();
        assertThat(widgets.getFirst().getWidgetConfig()).isNotNull();
    }

    @Test
    @DisplayName("위젯 ID와 대시보드 ID로 개별 위젯 조회 성공")
    void findByWidgetIdAndDashboardDashboardId_success() {
        // when
        Optional<Widget> found = widgetRepository.findByWidgetIdAndDashboardDashboardId(1L, 1L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getWidgetId()).isEqualTo(1L);
        assertThat(found.get().getWidth()).isEqualTo(6);
    }

    @Test
    @DisplayName("위젯 ID와 대시보드 ID로 개별 위젯 삭제 성공")
    void deleteByWidgetIdAndDashboardDashboardId_success() {
        // when
        widgetRepository.deleteByWidgetIdAndDashboardDashboardId(1L, 1L);

        // then
        Optional<Widget> found = widgetRepository.findByWidgetIdAndDashboardDashboardId(1L, 1L);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("대시보드 ID에 속한 모든 위젯 삭제 성공")
    void deleteAllByDashboardDashboardId_success() {
        // when
        widgetRepository.deleteAllByDashboardDashboardId(1L);

        // then
        List<Widget> widgets = widgetRepository.findAllByDashboardDashboardId(1L);
        assertThat(widgets).isEmpty();
    }
}
