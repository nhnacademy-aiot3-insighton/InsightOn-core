package com.insighton.core.adapter.client.actuator.lg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// LG ThinQ Connect "GET /devices" 응답 항목: { "deviceId": "...", "deviceInfo": { "deviceType": "AIRCON", "alias": "..." } }
@JsonIgnoreProperties(ignoreUnknown = true)
public record LgThinQDeviceListItem(String deviceId, DeviceInfo deviceInfo) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeviceInfo(String deviceType, String alias) {
    }
}
