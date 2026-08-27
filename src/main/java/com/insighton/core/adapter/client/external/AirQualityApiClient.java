package com.insighton.core.adapter.client.external;

import com.insighton.core.domain.weather.dto.AirQualityResponseDto;
import com.insighton.core.domain.weather.dto.AirQualityResponseDto.Item;
import com.insighton.core.domain.weather.exception.WeatherApiException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
                // 임시: 구 단위 매칭(cityName) 대신 시/도 전체 측정소 평균으로 대체
                // - 에어코리아 실시간 측정 API가 구 정보를 안 줘서 동 이름만으로는 구 매칭이 불가능함
                resultMap.put("pm10Value", averageOf(items, Item::pm10Value));
                resultMap.put("pm25Value", averageOf(items, Item::pm25Value));
                resultMap.put("pm10Value24", averageOf(items, Item::pm10Value24));
                resultMap.put("pm25Value24", averageOf(items, Item::pm25Value24));
                resultMap.put("pm10Grade1h", maxGradeOf(items, Item::pm10Grade1h));
                resultMap.put("pm25Grade1h", maxGradeOf(items, Item::pm25Grade1h));
            }
        }
        return resultMap;
    }

    // "-"나 null처럼 값 없는 측정소는 평균 계산에서 제외, 유효한 값이 하나도 없으면 "N/A"
    private String averageOf(List<Item> items, Function<Item, String> extractor) {
        double[] values = items.stream()
                .map(extractor)
                .filter(v -> v != null && !v.equals("-"))
                .mapToDouble(Double::parseDouble)
                .toArray();
        if (values.length == 0) {
            return "N/A";
        }
        return String.format("%.1f", Arrays.stream(values).average().orElse(0));
    }

    // 등급은 평균 내지 않고 매칭된 측정소 중 가장 나쁜(숫자가 큰) 값을 채택
    private String maxGradeOf(List<Item> items, Function<Item, String> extractor) {
        return items.stream()
                .map(extractor)
                .filter(v -> v != null && !v.equals("-"))
                .mapToInt(Integer::parseInt)
                .max()
                .stream()
                .mapToObj(String::valueOf)
                .findFirst()
                .orElse(null);
    }
}
