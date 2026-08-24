package com.insighton.core.controller.api;

import com.insighton.core.domain.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.usecase.chartdata.ChartDataUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChartDataController {
    private final ChartDataUseCase chartDataUseCase;

    /**
     * chart.js에서 주기적으로 호출할 API
     *
     * @param widgetId 관련 Widget ID
     * @return influxDB에서 query문을 날려 가져온 값을 dto에 담아 반환
     */
    @GetMapping("/api/v1/groups/{group-id}/location/{location-id}/dashboard/widgets/{widget-id}/chart-data")
    public ResponseEntity<ChartDataResponse> getWidgetChartData(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("location-id") Long locationId,
            @PathVariable("widget-id") Long widgetId
    ) {

        ChartDataResponse chartDataResponse = chartDataUseCase.getWidgetChartData(userId, groupId, locationId, widgetId);
        return ResponseEntity.ok(chartDataResponse);
    }
}
