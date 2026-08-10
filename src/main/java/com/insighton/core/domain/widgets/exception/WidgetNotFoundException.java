package com.insighton.core.domain.widgets.exception;

public class WidgetNotFoundException extends RuntimeException {
    public WidgetNotFoundException(String message) {
        super(message);
    }

    public static WidgetNotFoundException notFoundWidgetByDashboardId(Long dashboardId) {
        return new WidgetNotFoundException(
                String.format("Widget not found. Dashboard ID : " + dashboardId)
        );
    }

    public static WidgetNotFoundException notFoundWidgetByWidgetId(Long widgetId) {
        return new WidgetNotFoundException(
                String.format("Widget not found. Widget ID : " + widgetId)
        );
    }
}
