package com.insighton.core.rabbitmq.dto;

import java.time.Instant;
import java.util.Map;

public record TelemetryEventMessage(
    Long groupsId,
    Long locationsId,
    Long devicesId,
    Map<String, Object> metrics,
    Instant timestamp
) {}