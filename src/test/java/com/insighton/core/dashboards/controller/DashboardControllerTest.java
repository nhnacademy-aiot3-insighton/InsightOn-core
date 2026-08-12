package com.insighton.core.dashboards.controller;

import com.insighton.core.controller.api.DashboardController;
import com.insighton.core.domain.dashboards.dto.response.DashboardResponse;
import com.insighton.core.domain.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.usecase.DashboardSaveUseCase;
import com.insighton.core.usecase.DashboardUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardUseCase dashboardUseCase;

    @MockitoBean
    private DashboardSaveUseCase dashboardSaveUseCase;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("대시보드 조회 성공 - 200 OK 및 DashboardResponse 반환")
        void getDashboard_success() throws Exception {
            // given
            Long userId = 1L;
            Long groupId = 1L;
            Long locationId = 10L;

            DashboardResponse mockResponse = DashboardResponse.builder()
                    .dashboardId(100L)
                    .title("거실 - dashboard")
                    .widgetsList(List.of())
                    .build();

            given(dashboardUseCase.getDashboard(userId, groupId, locationId)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/groups/{group-id}/location/{location-id}/dashboard", groupId, locationId)
                            .header("X-USER-ID", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dashboardId").value(100L))
                    .andExpect(jsonPath("$.title").value("거실 - dashboard"));

            verify(dashboardUseCase).getDashboard(userId, groupId, locationId);
        }

        @Test
        @DisplayName("대시보드 저장 성공 - 200 OK 및 ChartData 맵 반환")
        void saveDashboard_success() throws Exception {
            // given
            Long userId = 1L;
            Long groupId = 1L;
            Long locationId = 10L;

            List<Long> widgetIds = List.of(1L);
            given(dashboardSaveUseCase.saveDashboard(eq(userId), eq(groupId), eq(locationId), any()))
                    .willReturn(widgetIds);

            ChartDataResponse mockChartData = ChartDataResponse.builder()
                    .labels(List.of("10:00", "10:15"))
                    .datasets(List.of())
                    .build();

            given(dashboardSaveUseCase.saveDashboardInfluxDB(widgetIds))
                    .willReturn(Map.of(1L, mockChartData));

            // when & then
            mockMvc.perform(post("/api/v1/groups/{group-id}/location/{location-id}/dashboard/save", groupId, locationId)
                            .header("X-USER-ID", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$['1'].labels.length()").value(2));

            verify(dashboardSaveUseCase).saveDashboard(eq(userId), eq(groupId), eq(locationId), any());
            verify(dashboardSaveUseCase).saveDashboardInfluxDB(widgetIds);
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailureCases {

        @Test
        @DisplayName("필수 헤더(X-USER-ID) 누락 시 400 Bad Request")
        void getDashboard_missingHeader_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/groups/{group-id}/location/{location-id}/dashboard", 1L, 10L))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("대시보드 저장 시 필수 헤더(X-USER-ID) 누락 시 400 Bad Request")
        void saveDashboard_missingHeader_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/groups/{group-id}/location/{location-id}/dashboard/save", 1L, 10L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isBadRequest());
        }
    }
}
