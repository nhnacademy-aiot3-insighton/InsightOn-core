package com.insighton.core.domain.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// 기상청 중기기온조회(getMidTa) 전용 응답 - 단기/초단기예보와 달리 category/fcstValue 쌍이 아니라
// 일자별(taMinN/taMaxN) 필드가 평평하게 나열되는 별도 구조라 KmaWeatherResponseDto와 분리함
@JsonIgnoreProperties(ignoreUnknown = true)
public record MidTermTemperatureResponseDto(Response response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Items items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<Item> item) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String regId,
            String taMin4, String taMax4,
            String taMin5, String taMax5,
            String taMin6, String taMax6,
            String taMin7, String taMax7,
            String taMin8, String taMax8,
            String taMin9, String taMax9,
            String taMin10, String taMax10
    ) {
    }
}
