package com.insighton.core.adapter.client.actuator.smartthings.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// SmartThings "List devices" 응답: { "items": [ { "deviceId": "...", "label": "...", "type": "AIRCON" } ] }
@JsonIgnoreProperties(ignoreUnknown = true)
public record SmartThingsDeviceListResponse(List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String deviceId, String label, String type) {
    }
}
