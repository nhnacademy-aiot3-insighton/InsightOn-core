package com.insighton.core.domain.weather.dto;

public record AirQualityDto(
        String pm10Value, // 미세먼지 농도
        String pm25Value, // 초미세먼지 농도
        String pm10Value24, // 미세먼지 24시간 예측농도
        String pm25Value24, // 초미세먼지 24시간 예측농도
        String pm10Grade, // 미세먼지 등급
        String pm25Grade // 초미세먼지 등급
) {
}
