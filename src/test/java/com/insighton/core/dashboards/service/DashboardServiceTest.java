package com.insighton.core.dashboards.service;

import com.insighton.core.domain.dashboards.dto.request.DashboardRequest;
import com.insighton.core.domain.dashboards.dto.response.DashboardResponse;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.exception.DashboardNotFoundException;
import com.insighton.core.domain.dashboards.repository.DashboardRepository;
import com.insighton.core.domain.dashboards.service.impl.DashboardServiceImpl;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.widgets.dto.response.WidgetsListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("대시보드 생성 성공")
        void createDashboard_success() {
            // given
            Location mockLocation = mock(Location.class);
            DashboardRequest request = new DashboardRequest(1L, "거실 - dashboard");

            // when
            dashboardService.createDashboard(mockLocation, request);

            // then
            verify(dashboardRepository, times(1)).save(any(Dashboard.class));
        }

        @Test
        @DisplayName("대시보드 Response 조회 성공")
        void getDashboard_success() {
            // given
            Long locationId = 1L;
            Dashboard mockDashboard = mock(Dashboard.class);
            given(mockDashboard.getDashboardId()).willReturn(10L);
            given(mockDashboard.getTitle()).willReturn("거실 - dashboard");
            given(dashboardRepository.findByLocationLocationId(locationId)).willReturn(Optional.of(mockDashboard));

            List<WidgetsListResponse> widgetsList = List.of(mock(WidgetsListResponse.class));

            // when
            DashboardResponse response = dashboardService.getDashboard(locationId, widgetsList);

            // then
            assertThat(response).isNotNull();
            assertThat(response.dashboardId()).isEqualTo(10L);
            assertThat(response.title()).isEqualTo("거실 - dashboard");
            assertThat(response.widgetsList()).hasSize(1);
        }

        @Test
        @DisplayName("대시보드 제목 수정 성공")
        void updateDashboardTitle_success() {
            // given
            DashboardRequest request = new DashboardRequest(1L, "새 대시보드 제목");
            Dashboard mockDashboard = mock(Dashboard.class);
            given(dashboardRepository.findByLocationLocationId(1L)).willReturn(Optional.of(mockDashboard));

            // when
            dashboardService.updateDashboardTitle(request);

            // then
            verify(mockDashboard, times(1)).updateTitle("새 대시보드 제목");
        }

        @Test
        @DisplayName("대시보드 삭제 성공")
        void deleteDashboard_success() {
            // given
            Long locationId = 1L;
            Dashboard mockDashboard = mock(Dashboard.class);
            given(dashboardRepository.findByLocationLocationId(locationId)).willReturn(Optional.of(mockDashboard));

            // when
            dashboardService.deleteDashboard(locationId);

            // then
            verify(dashboardRepository, times(1)).delete(mockDashboard);
        }

        @Test
        @DisplayName("LocationId로 대시보드 엔티티 조회 성공")
        void getDashboardByLocationId_success() {
            // given
            Long locationId = 1L;
            Dashboard mockDashboard = mock(Dashboard.class);
            given(dashboardRepository.findByLocationLocationId(locationId)).willReturn(Optional.of(mockDashboard));

            // when
            Dashboard result = dashboardService.getDashboardByLocationId(locationId);

            // then
            assertThat(result).isEqualTo(mockDashboard);
        }

        @Test
        @DisplayName("getDashboardEntity 메서드 조회 성공")
        void getDashboardEntity_success() {
            // given
            Long locationId = 1L;
            Dashboard mockDashboard = mock(Dashboard.class);
            given(dashboardRepository.findByLocationLocationId(locationId)).willReturn(Optional.of(mockDashboard));

            // when
            Dashboard result = dashboardService.getDashboardEntity(locationId);

            // then
            assertThat(result).isEqualTo(mockDashboard);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCases {

        @Test
        @DisplayName("대시보드 조회 실패 - 대시보드가 존재하지 않을 때 DashboardNotFoundException 발생")
        void getDashboard_notFound() {
            // given
            Long locationId = 999L;
            given(dashboardRepository.findByLocationLocationId(locationId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> dashboardService.getDashboard(locationId, List.of()))
                    .isInstanceOf(DashboardNotFoundException.class);
        }

        @Test
        @DisplayName("대시보드 제목 수정 실패 - 대시보드가 존재하지 않을 때 DashboardNotFoundException 발생")
        void updateDashboardTitle_notFound() {
            // given
            DashboardRequest request = new DashboardRequest(999L, "새 제목");
            given(dashboardRepository.findByLocationLocationId(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> dashboardService.updateDashboardTitle(request))
                    .isInstanceOf(DashboardNotFoundException.class);
        }

        @Test
        @DisplayName("대시보드 삭제 실패 - 대시보드가 존재하지 않을 때 DashboardNotFoundException 발생")
        void deleteDashboard_notFound() {
            // given
            Long locationId = 999L;
            given(dashboardRepository.findByLocationLocationId(locationId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> dashboardService.deleteDashboard(locationId))
                    .isInstanceOf(DashboardNotFoundException.class);
        }
    }
}
