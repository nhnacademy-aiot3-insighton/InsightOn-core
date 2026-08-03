package com.insighton.core.dashboards.dto.request;

import lombok.Builder;

/**
 * dashboards 응답 요청
 *
 * @param locationId 대시보드가 소속된 대상 장소(오피스 구역) 식별 ID (1:1 관계 FK)
 * @param title      웹 브라우저 화면 상단에 표시될 대시보드 메인 헤드라인 제목 명칭
 */
@Builder
public record DashboardsRequest(
        Long locationId,
        String title) {
}
