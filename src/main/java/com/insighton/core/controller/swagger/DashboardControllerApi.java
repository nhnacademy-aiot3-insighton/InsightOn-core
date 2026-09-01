package com.insighton.core.controller.swagger;

import com.insighton.core.domain.dashboards.dto.response.DashboardResponse;
import com.insighton.core.domain.widgets.dto.chart.ChartDataResponse;
import com.insighton.core.domain.widgets.dto.request.WidgetSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Swagger 문서화 전용 인터페이스 - 대시보드(Dashboard) 관리
 */
@Tag(name = "대시보드 API (Dashboard)", description = "대시보드 상세 조회 및 위젯 생성/수정/삭제 일괄 저장 API")
public interface DashboardControllerApi {

    @Operation(summary = "대시보드 조회", description = "특정 위치에 연결된 대시보드 정보 및 배치된 위젯 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "대시보드 조회 성공")
    @ApiResponse(responseCode = "403", description = "대시보드 접근 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 위치 또는 대시보드")
    ResponseEntity<DashboardResponse> getDashboard(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "위치 ID", required = true) Long locationId
    );

    @Operation(summary = "대시보드 위젯 일괄 저장 (생성/수정/삭제)", description = "대시보드 내 위젯들의 배치 좌표, 크기 및 설정 변경사항을 일괄 저장합니다. (Manager 이상 권한 필요)")
    @ApiResponse(responseCode = "200", description = "대시보드 위젯 일괄 저장 성공 및 InfluxDB 차트 데이터 반환")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 위젯 배치 좌표/크기 또는 잘못된 요청")
    @ApiResponse(responseCode = "403", description = "대시보드 저장 권한 없음 (Manager 이상 권한 필요)")
    @ApiResponse(responseCode = "409", description = "위젯 저장 동시성 충돌 (동시 수정 발생 시)")
    ResponseEntity<Map<Long, ChartDataResponse>> saveDashboard(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "위치 ID", required = true) Long locationId,
            @Valid List<@Valid WidgetSaveRequest> requests
    );
}
