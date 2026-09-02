package com.insighton.core.adapter.client.actuator.lg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// LG ThinQ Connect 제어 응답의 핵심 필드.
// 성공: { "messageId": "<id>" }  /  오류: { "error": { "code": "...", "message": "..." } }
@JsonIgnoreProperties(ignoreUnknown = true)
public record LgThinQControlResponse(String messageId, Error error) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(String code, String message) {
    }
}
