package com.insighton.core.domain.weather.dto;

public record CurrentWeatherDto(
        String temp, // 현재기온
        String humidity, // 습도
        String hourlyRainFall, // 1시간 강수량
        String precipitationType // 강수 형태
) {
}
