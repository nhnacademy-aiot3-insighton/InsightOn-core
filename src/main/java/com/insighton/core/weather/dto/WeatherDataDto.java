package com.insighton.core.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherDataDto(
        //기상청 단기예보 + 실황 통합
        @JsonProperty("TMP") String temp, // 현재 기온
        @JsonProperty("TMX") String maxTemp, // 최고 기온
        @JsonProperty("TMN") String minTemp, // 최저 기온
        @JsonProperty("SKY") String skyStatus, // 하늘 상태
        @JsonProperty("PTY") String precipitationType, // 강수 형태
        @JsonProperty("POP") String rainPrecipitation, // 강수 유무/확률
        @JsonProperty("PCP") String hourlyRainfall, // 1시간 강수량
        @JsonProperty("REH") String humidity, // 상대 습도
        //공공데이터포털 미세먼지 항목
        String pm10Value,
        String pm25Value,
        String pm10Value24,
        String pm25Value24,
        String pm10Grade1h,
        String pm25Grade1h
) {
}
