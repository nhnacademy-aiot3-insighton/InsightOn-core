package com.insighton.core.domain.weather.dto;

public record ForecastWeatherDto(
        String temperature,
        String maxTemp, // 오늘 최고 기온
        String minTemp, // 오늘 최저 기온
        String skyStatus, // 하늘 상태
        String rainPrecipitation, // 강수 확률
        String forecastPrecipitationType // 예보상 강수 형태
) {
}