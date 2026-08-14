package com.insighton.core.adapter.client.external;

import com.insighton.core.domain.weather.dto.AirQualityResponseDto;
import com.insighton.core.domain.weather.dto.AirQualityResponseDto.Item;
import com.insighton.core.domain.weather.exception.WeatherApiException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AirQualityApiClient {

    private final RestClient airQualityRestClient;

    @Value("${weather.api.air-base-url}")
    private String airBaseUrl;

    @Value("${weather.api.air-key}")
    private String airApiKey;

    public Map<String, String> fetchAirQualityData(String sidoName, String cityName) {
        String encodedSidoName = URLEncoder.encode(sidoName, StandardCharsets.UTF_8);
        String fullUrl = String.format(
                "%s/getCtprvnRltmMesureDnsty?serviceKey=%s&sidoName=%s&pageNo=1&numOfRows=100&ver=1.3&returnType=json",
                airBaseUrl, airApiKey, encodedSidoName
        );

        AirQualityResponseDto response;
        try {
            response = airQualityRestClient.get()
                    .uri(URI.create(fullUrl))
                    .retrieve()
                    .body(AirQualityResponseDto.class);
        } catch (Exception e) {
            throw new WeatherApiException("에어코리아 API 호출 중 오류 발생");
        }

        Map<String, String> resultMap = new HashMap<>();
        if (response != null && response.response() != null && response.response().body() != null) {
            List<Item> items = response.response().body().items();
            if (items != null && !items.isEmpty()) {
                Item selectedItem = items.stream()
                        .filter(item -> item.cityName() != null && item.cityName().equals(cityName))
                        .findFirst()
                        .orElseGet(() -> items.stream()
                                .filter(item -> item.stationName() != null && item.stationName().contains(cityName))
                                .findFirst()
                                .orElseThrow(
                                        () -> new WeatherApiException("해당 지역의 미세먼지 측정소 정보를 찾을 수 없습니다.: " + cityName)));

                resultMap.put("pm10Value", selectedItem.pm10Value());
                resultMap.put("pm25Value", selectedItem.pm25Value());
                resultMap.put("pm10Value24", selectedItem.pm10Value24());
                resultMap.put("pm25Value24", selectedItem.pm25Value24());
                resultMap.put("pm10Grade1h", selectedItem.pm10Grade1h());
                resultMap.put("pm25Grade1h", selectedItem.pm25Grade1h());
            }
        }
        return resultMap;
    }
}
