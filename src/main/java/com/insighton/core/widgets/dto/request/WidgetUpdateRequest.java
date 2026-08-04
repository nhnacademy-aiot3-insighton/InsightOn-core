package com.insighton.core.widgets.dto.request;

import com.insighton.core.widgets.entity.WidgetConfig;
import lombok.Builder;

/**
 * widget으로 보고 싶은 값을 수정했을 때의 요청
 */
@Builder
public record WidgetUpdateRequest(
        WidgetConfig widgetConfig) {
}
