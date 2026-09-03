package com.insighton.core.domain.weather.dto;

public record WeatherDataDto(
        CurrentWeatherDto current, // 초단기실황
        ForecastWeatherDto forecast, // 단기예보
        UltraForecastWeatherDto ultraForecastWeather, // 초단기예보
        AirQualityDto airQuality, // 미세먼지
        MidTermTemperatureDto midTermTemperature // 중기기온전망 (4~10일 후)
) {
}
