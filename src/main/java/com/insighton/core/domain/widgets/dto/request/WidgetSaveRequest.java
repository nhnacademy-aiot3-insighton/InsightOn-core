package com.insighton.core.domain.widgets.dto.request;

import com.insighton.core.domain.widgets.entity.WidgetConfig;
import jakarta.validation.constraints.Min;
import lombok.Builder;

/**
 * 한번에 저장할 때 받아올 dto
 *
 * @param widgetId     생성되는 widget이라면 null 값으로 들어오고 수정이라면 값이 들어옴
 * @param xPos         그리드 레이아웃 상에서의 가로 시작 그리드 번호 위치 (X 좌표)
 * @param yPos         그리드 레이아웃 상에서의 세로 시작 그리드 번호 위치 (Y 좌표)
 * @param width        위젯 컴포넌트가 가로로 차지하는 격자 폭 셀(Cell) 크기
 * @param height       위젯 컴포넌트가 세로로 차지하는 격자 높이 셀(Cell) 크기
 * @param widgetConfig influxDB에 query를 날리기 위한 값들
 */
@Builder
public record WidgetSaveRequest(
        Long widgetId, // << 이거 null이면 생성 요청, null이 아니라면 수정 요청
        @Min(value = 0, message = "xPos는 0 이상이어야 합니다.")
        Integer xPos,
        @Min(value = 0, message = "yPos는 0 이상이어야 합니다.")
        Integer yPos,
        @Min(value = 1, message = "width는 1 이상이어야 합니다.")
        Integer width,
        @Min(value = 1, message = "height는 1 이상이어야 합니다.")
        Integer height,
        WidgetConfig widgetConfig
) {
}
