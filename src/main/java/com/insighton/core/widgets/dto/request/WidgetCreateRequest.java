package com.insighton.core.widgets.dto.request;

import com.insighton.core.widgets.entity.WidgetConfig;
import lombok.Builder;

/**
 * 위젯 생성 요청 DTO
 *
 * @param dashboardId widget이 속해있는 dashboard ID
 * @param xPos        그리드 레이아웃 상에서의 가로 시작 그리드 번호 위치 (X 좌표)
 * @param yPos        그리드 레이아웃 상에서의 세로 시작 그리드 번호 위치 (Y 좌표)
 * @param width       위젯 컴포넌트가 가로로 차지하는 격자 폭 셀(Cell) 크기
 * @param height      위젯 컴포넌트가 세로로 차지하는 격자 높이 셀(Cell) 크기
 */
@Builder
public record WidgetCreateRequest(
        Long dashboardId,
        int xPos,
        int yPos,
        int width,
        int height,
        WidgetConfig widgetConfig
) {
}
