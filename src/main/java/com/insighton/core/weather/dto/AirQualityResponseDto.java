package com.insighton.core.weather.dto;

import java.util.List;

public record AirQualityResponseDto(Response response) {

    public record Response(Header header, Body body) {
    }

    public record Header(String resultCode, String resultMsg) {
    }

    public record Body(List<Item> items, Integer totalCount) {
    }

    public record Item(
            String stationName,
            String cityName,
            String pm10Value,
            String pm25Value,
            String pm10Value24,
            String pm25Value24,
            String pm10Grade1h,
            String pm25Grade1h
    ) {
    }
}
