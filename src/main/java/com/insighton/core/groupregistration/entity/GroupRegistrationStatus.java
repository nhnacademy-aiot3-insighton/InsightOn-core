package com.insighton.core.groupregistration.entity;

import lombok.Getter;

@Getter
public enum GroupRegistrationStatus {
    PENDING("대기중"),
    APPROVED("승인됨"),
    REJECTED("거절됨"),
    CANCELLED("취소됨");

    private final String label;

    GroupRegistrationStatus(String label) {
        this.label = label;
    }
}
