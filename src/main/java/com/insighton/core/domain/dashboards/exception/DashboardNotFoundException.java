package com.insighton.core.domain.dashboards.exception;

public class DashboardNotFoundException extends RuntimeException {
    public DashboardNotFoundException(Long locationId) {
        super("not found dashboard. location ID : " + locationId);
    }
}
