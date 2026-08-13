package com.insighton.core.widgets.controller;

import com.insighton.core.controller.api.ChartDataController;
import com.insighton.core.domain.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.domain.widgets.exception.WidgetNotFoundException;
import com.insighton.core.domain.widgets.service.WidgetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChartDataController.class)
class ChartDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WidgetService widgetService;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("위젯 차트 데이터 조회 성공 - 200 OK 및 ChartDataResponse 반환")
        void getWidgetChartData_success() throws Exception {
            // given
            Long widgetId = 1L;
            ChartDataResponse mockResponse = ChartDataResponse.builder()
                    .labels(List.of("10:00", "10:15"))
                    .datasets(List.of())
                    .build();

            given(widgetService.getWidgetChartData(widgetId)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/dashboard/widgets/{widget-id}/chart-data", widgetId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.labels.length()").value(2))
                    .andExpect(jsonPath("$.labels[0]").value("10:00"));

            verify(widgetService).getWidgetChartData(widgetId);
        }
    }

    @Nested
    @DisplayName("실패 및 예외 케이스")
    class FailureCases {

        @Test
        @DisplayName("PathVariable 타입 오류 시 400 Bad Request")
        void invalidPathVariable_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/widgets/{widget-id}/chart-data", "invalid-id"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 위젯 ID 조회 시 예외 발생")
        void widgetNotFound_returnsError() throws Exception {
            // given
            Long widgetId = 999L;
            given(widgetService.getWidgetChartData(widgetId))
                    .willThrow(WidgetNotFoundException.notFoundWidgetByWidgetId(widgetId));

            // when & then
            mockMvc.perform(get("/api/v1/dashboard/widgets/{widget-id}/chart-data", widgetId))
                    .andExpect(status().isNotFound());
        }
    }
}
