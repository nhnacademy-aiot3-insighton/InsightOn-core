package com.insighton.core.controller.swagger;

import com.insighton.core.domain.widgets.dto.chart.ChartDataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/**
 * Swagger 문서화 전용 인터페이스 - 차트 데이터(ChartData) 관리
 */
@Tag(name = "차트 데이터 API (ChartData)", description = "위젯 개별 InfluxDB 시계열 차트 데이터 조회 API")
public interface ChartDataControllerApi {

    @Operation(summary = "위젯 차트 시계열 데이터 조회", description = "단일 위젯의 InfluxDB 시계열 데이터를 조회하여 Chart.js 렌더링용 라벨 및 데이터셋을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "차트 데이터 조회 성공")
    @ApiResponse(responseCode = "403", description = "차트 데이터 조회 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 위젯 또는 초기 데이터 가공 중")
    ResponseEntity<ChartDataResponse> getWidgetChartData(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "위치 ID", required = true) Long locationId,
            @Parameter(description = "위젯 ID", required = true) Long widgetId
    );
}
