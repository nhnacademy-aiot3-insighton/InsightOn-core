package com.insighton.core.widgets.dto.chart;

import java.util.List;

/**
 * 차트 데이터셋 DTO
 *
 * @param labels field 이름 (예시 -> "temperature", "humidity")
 * @param data   측정 값 배열 ([23.5, 23.8, 24.1])
 */
public record ChartDataset(
        String labels,
        List<Object> data
) {
}
