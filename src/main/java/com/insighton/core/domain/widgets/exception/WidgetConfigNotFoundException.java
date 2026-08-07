package com.insighton.core.domain.widgets.exception;

public class WidgetConfigNotFoundException extends RuntimeException {
    public WidgetConfigNotFoundException(Long widgetId) {
        super("widget config not found. widget ID [" + widgetId + "]. method name : getWidgetConfigFromCache");
    }
}
