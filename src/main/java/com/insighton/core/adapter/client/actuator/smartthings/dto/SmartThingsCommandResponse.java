package com.insighton.core.adapter.client.actuator.smartthings.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// SmartThings "Execute commands" 응답의 핵심 필드.
// { "results": [ { "id": "<uuid>", "status": "ACCEPTED" } ] }
// 실제 응답에는 다른 필드가 더 있을 수 있으므로 unknown 무시.
@JsonIgnoreProperties(ignoreUnknown = true)
public record SmartThingsCommandResponse(List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(String id, String status) {
    }
}
