package com.insighton.core.controller.api;

import com.insighton.core.controller.swagger.ChartDataControllerApi;
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
public class ChartDataController implements ChartDataControllerApi {
    private final ChartDataUseCase chartDataUseCase;

    @Override
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
