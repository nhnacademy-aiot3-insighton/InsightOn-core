package com.insighton.core.adapter.client.actuator.lg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// LG ThinQ Connect "device control" 응답.
// 성공: { "messageId": "<uuid>", "timestamp": "2026-09-03T12:00:00Z", "response": {} }
// 오류: { "error": { "code": "0110", "message": "..." } }
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
