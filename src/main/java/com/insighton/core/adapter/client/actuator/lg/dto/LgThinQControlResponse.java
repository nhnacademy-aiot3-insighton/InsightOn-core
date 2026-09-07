package com.insighton.core.adapter.client.actuator.lg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LgThinQControlResponse(
        String messageId,
        String timestamp,
        Object response,
        Error error
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(String code, String message) {
    }
}
