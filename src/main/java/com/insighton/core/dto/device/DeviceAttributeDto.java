package com.insighton.core.dto.device;

public record DeviceAttributeDto(
        String metricKey,
        String displayName,
        String unit,
        String currentValueStr
) {}
