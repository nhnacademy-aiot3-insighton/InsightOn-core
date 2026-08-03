package com.insighton.core.widgets.dto;

import lombok.Builder;

/**
 * 임시 widget list
 *
 * @param dashboardId
 * @param widgetId
 * @param type
 * @param metricKey
 * @param xPos
 * @param yPos
 * @param width
 * @param height
 */
@Builder
public record WidgetsListResponse(
        Long dashboardId,
        Long widgetId,
        String type,
        String metricKey,
        int xPos,
        int yPos,
        int width,
        int height
) {
}
