package com.insighton.core.weather.dto;

import java.util.List;

public record kmaWeatherResponseDto(Response response) {

    public record Response(Header header, Body body) {
    }

    public record Header(String resultCode, String resultMsg) {
    }

    public record Body(Items items) {
    }

    public record Items(List<Item> item) {
    }

    public record Item(
            String category,
            String fcstDate, // 예보 날짜
            String fcstTime, // 예보 시각
            String fcstValue, // 예보 값
            String obsrValue // 관측 값 (실황용)
    ) {
    }
}
