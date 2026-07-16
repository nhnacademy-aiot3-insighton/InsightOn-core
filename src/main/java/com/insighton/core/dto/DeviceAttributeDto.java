package com.insighton.core.dto;

public record DeviceAttributeDto(
        String metricKey,
        String displayName,
        String unit,
        String currentValueStr
) {
}
