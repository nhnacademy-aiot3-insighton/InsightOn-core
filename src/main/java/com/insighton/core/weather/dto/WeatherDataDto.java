package com.insighton.core.weather.dto;

public record WeatherDataDto(
        CurrentWeather current, // 초단기실황
        ForecastWeather forecast, // 단기예보
        AirQuality airQuality // 미세먼지
) {

    public record CurrentWeather(
            String temp, // 현재기온
            String humidity, // 습도
            String hourlyRainFall, // 1시간 강수량
            String precipitationType // 강수 형태
    ) {
    }

    public record ForecastWeather(
            String maxTemp, // 오늘 최고 기온
            String minTemp, // 오늘 최저 기온
            String skyStatus, // 하늘 상태
            String rainPrecipitation, // 강수 확률
            String forecastPrecipitationType // 예보상 강수 형태
    ) {
    }

    public record AirQuality(
            String pm10Value, // 미세먼지 농도
            String pm25Value, // 초미세먼지 농도
            String pm10Value24, // 미세먼지 24시간 예측농도
            String pm25Value24, // 초미세먼지 24시간 예측농도
            String pm10Grade, // 미세먼지 등급
            String pm25Grade // 초미세먼지 등급
    ) {
    }
}
