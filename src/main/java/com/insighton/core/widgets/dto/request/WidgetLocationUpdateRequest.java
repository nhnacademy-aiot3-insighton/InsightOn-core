package com.insighton.core.widgets.dto.request;

import lombok.Builder;

/**
 * widget의 크기나 위치를 이동하고 수정했을 때 요청
 *
 * @param widgetId 수정하려는 widget의 ID
 * @param xPos     그리드 레이아웃 상에서의 가로 시작 그리드 번호 위치 (X 좌표)
 * @param yPos     그리드 레이아웃 상에서의 세로 시작 그리드 번호 위치 (Y 좌표)
 * @param width    위젯 컴포넌트가 가로로 차지하는 격자 폭 셀(Cell) 크기
 * @param height   위젯 컴포넌트가 세로로 차지하는 격자 높이 셀(Cell) 크기
 */
@Builder
public record WidgetLocationUpdateRequest(
        Long widgetId,
        int xPos,
        int yPos,
        int width,
        int height
) {
}
