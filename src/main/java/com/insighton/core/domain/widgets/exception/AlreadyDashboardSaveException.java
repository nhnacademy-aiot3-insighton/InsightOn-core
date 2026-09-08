package com.insighton.core.domain.widgets.exception;

public class AlreadyDashboardSaveException extends RuntimeException {
    public AlreadyDashboardSaveException(Long dashboardId) {
        super("최근에 이미 저장이 반영된 dashboard 입니다. dashboard ID : " + dashboardId);
    }
}
