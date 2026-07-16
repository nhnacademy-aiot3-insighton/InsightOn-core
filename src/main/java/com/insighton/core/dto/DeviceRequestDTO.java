package com.insighton.core.dto;


public record DeviceRequestDTO(
    String name,
    String type,
    String deviceEui,
    Long gatewayId,
    Long locationsId
) {}