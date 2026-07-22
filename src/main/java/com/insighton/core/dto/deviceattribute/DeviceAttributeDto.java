package com.insighton.core.dto.deviceattribute;

public record DeviceAttributeDto(
        String metricKey,
        String displayName,
        String unit,
        String currentValueStr
) {}
