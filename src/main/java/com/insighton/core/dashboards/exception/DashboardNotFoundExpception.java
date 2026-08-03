package com.insighton.core.dashboards.exception;

public class DashboardNotFoundExpception extends RuntimeException {
    public DashboardNotFoundExpception(Long locationId) {
        super("not found dashboard. location ID : " + locationId);
    }
}
