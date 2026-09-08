package com.insighton.core.domain.weather.dto;

public record MidTermTemperatureDto(
        String avgMaxTemp, // 4~10일 후 평균 최고기온 전망
        String avgMinTemp  // 4~10일 후 평균 최저기온 전망
) {
}

