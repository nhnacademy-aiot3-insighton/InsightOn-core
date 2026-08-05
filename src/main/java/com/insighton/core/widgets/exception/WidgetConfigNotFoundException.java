package com.insighton.core.widgets.exception;

public class WidgetConfigNotFoundException extends RuntimeException {
    public WidgetConfigNotFoundException(Long widgetId) {
        super("widget config not found. widget ID [" + widgetId + "]. method name : getWidgetConfigFromCache");
    }
}
