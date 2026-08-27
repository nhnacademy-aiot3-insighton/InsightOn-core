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

        // 응답 구조 및 items 유효성 방어 검증 추가
        if (response == null || response.response() == null || response.response().body() == null) {
            throw new WeatherApiException("기상청 API 응답 구조를 파악할 수 없습니다.");
        }

        KmaWeatherResponseDto.Body body = response.response().body();
        if (body.items() == null || body.items().item() == null) {
            return resultMap; // 데이터가 없는 경우 빈 맵 반환 (컨트랙트 준수)
        }

        List<Item> items = body.items().item();
        if (items.isEmpty()) {
            return resultMap;
        }

        if (isNcst) {
            for (Item item : items) {
                resultMap.putIfAbsent(item.category(), item.obsrValue());
            }
        } else {
            // 단기예보 / 초단기예보의 경우 첫 번째 아이템의 예보일자(fcstDate)와 예보시각(fcstTime)을 기준으로 필터링
            String targetFcstDate = items.get(0).fcstDate();
            String targetFcstTime = items.get(0).fcstTime();

            for (Item item : items) {
                if (targetFcstDate.equals(item.fcstDate()) && targetFcstTime.equals(item.fcstTime())) {
                    resultMap.putIfAbsent(item.category(), item.fcstValue());
                }
            }

            // TMX(최고기온)/TMN(최저기온)은 하루 중 특정 시각(각각 15시/06시 발표분)에만 실려서
            // 위 필터(targetFcstTime과 정확히 일치)엔 걸리지 않는 경우가 많음 - 날짜만 같으면
            // 시각 무관하게 별도로 채움
            for (Item item : items) {
                if (targetFcstDate.equals(item.fcstDate())
                        && ("TMX".equals(item.category()) || "TMN".equals(item.category()))) {
                    resultMap.putIfAbsent(item.category(), item.fcstValue());
                }
            }
        }

        return resultMap;
    }
}