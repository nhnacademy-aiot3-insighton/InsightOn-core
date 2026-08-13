package com.insighton.core.adapter.client.external;

import com.insighton.core.domain.weather.dto.KmaWeatherResponseDto;
import com.insighton.core.domain.weather.dto.KmaWeatherResponseDto.Item;
import com.insighton.core.domain.weather.exception.WeatherApiException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KmaWeatherApiClient {

    private final RestClient kmaRestClient;

    @Value("${weather.api.kma-base-url}")
    private String kmaBaseUrl;

    @Value("${weather.api.kma-key}")
    private String kmaApiKey;

    public Map<String, String> fetchKmaApi(String endpoint, int nx, int ny, String baseDate, String baseTime,
                                           boolean isNcst) {
        String fullUrl = String.format(
                "%s%s?serviceKey=%s&pageNo=1&numOfRows=1000&dataType=JSON&base_date=%s&base_time=%s&nx=%s&ny=%s",
                kmaBaseUrl, endpoint, kmaApiKey, baseDate, baseTime, nx, ny
        );

        KmaWeatherResponseDto response;
        try {
            response = kmaRestClient.get()
                    .uri(URI.create(fullUrl))
                    .retrieve()
                    .body(KmaWeatherResponseDto.class);
        } catch (Exception e) {
            throw new WeatherApiException("기상청 API 호출 중 오류 발생");
        }

        Map<String, String> resultMap = new HashMap<>();
        if (response != null && response.response() != null && response.response().body() != null) {
            List<Item> items = response.response().body().items().item();
            if (items != null) {
                for (KmaWeatherResponseDto.Item item : items) {
                    String value = isNcst ? item.obsrValue() : item.fcstValue();
                    resultMap.putIfAbsent(item.category(), value);
                }
            }
        }
        return resultMap;
    }
}
