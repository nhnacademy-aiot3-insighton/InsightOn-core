package com.insighton.core.dto.deviceAttribute;

public record DeviceAttributeDto(
        String metricKey,
        String displayName,
        String unit,
        String currentValueStr
) {}
