package com.insighton.core.weather.service;

import com.insighton.core.exception.WeatherApiException;
import com.insighton.core.weather.dto.AirQualityResponseDto;
import com.insighton.core.weather.dto.KmaWeatherResponseDto;
import com.insighton.core.weather.dto.WeatherDataDto;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class WeatherIntegrationService {

    private final WebClient kmaWebClient;
    private final WebClient airQualityWebClient;

    @Value("${weather.api.kma-key}")
    private String kmaApiKey;

    @Value("${weather.api.air-key}")
    private String airApiKey;

    public WeatherDataDto fetchWeatherData(int girdX, int gridY, String stationName, String baseDate, String baseTime) {
        try {
            Map<String, String> currentMap = fetchKmaApi("/getUltraSrtNcst", girdX, gridY, baseDate, baseTime, true);

            String fcstBaseTime = getRecentForecastBaseTime(baseDate, baseTime);
            Map<String, String> forecastMap = fetchKmaApi("/getVilageFcst", girdX, gridY, baseDate, fcstBaseTime,
                    false);
            Map<String, String> airMap = fetchAirQualityData(stationName);

            return new WeatherDataDto(
                    currentMap.getOrDefault("T1H", forecastMap.getOrDefault("TMP", "N/A")),
                    forecastMap.getOrDefault("TMX", "N/A"),
                    forecastMap.getOrDefault("TMN", "N/A"),
                    forecastMap.getOrDefault("SKY", "N/A"),
                    currentMap.getOrDefault("PTY", forecastMap.getOrDefault("PTY", "N/A")),
                    forecastMap.getOrDefault("POP", "N/A"),
                    currentMap.getOrDefault("RN1", forecastMap.getOrDefault("PCP", "N/A")),
                    currentMap.getOrDefault("REH", forecastMap.getOrDefault("REH", "N/A")),
                    airMap.getOrDefault("pm10Value", "N/A"),
                    airMap.getOrDefault("pm25Value", "N/A"),
                    airMap.getOrDefault("pm10Value24", "N/A"),
                    airMap.getOrDefault("pm25Value24", "N/A"),
                    airMap.getOrDefault("pm10Grade1h", "N/A"),
                    airMap.getOrDefault("pm25Grade1h", "N/A")
            );
        } catch (Exception e) {
            throw new WeatherApiException("날씨/미세먼지 외부 API 연동 중 오류 발생", e);
        }
    }

    private Map<String, String> fetchKmaApi(String endpoint, int nx, int ny, String baseDate, String baseTime,
                                            boolean isNcst) {
        URI uri = UriComponentsBuilder.fromPath(endpoint)
                .queryParam("serviceKey", kmaApiKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1000)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true)
                .toUri();

        KmaWeatherResponseDto response = kmaWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(KmaWeatherResponseDto.class)
                .block();

        Map<String, String> resultMap = new HashMap<>();
        if (response != null && response.response() != null && response.response().body() != null) {
            List<KmaWeatherResponseDto.Item> items = response.response().body().items().item();
            if (items != null) {
                for (KmaWeatherResponseDto.Item item : items) {
                    String value = isNcst ? item.obsrValue() : item.fcstValue();
                    resultMap.putIfAbsent(item.category(), value);
                }
            }
        }
        return resultMap;
    }

    private Map<String, String> fetchAirQualityData(String stationName) {
        URI uri = UriComponentsBuilder.fromPath("/getMsrstnAcctoRltmMesureDnsty")
                .queryParam("serviceKey", airApiKey)
                .queryParam("stationName", stationName)
                .queryParam("dataTerm", "DAILY")
                .queryParam("ver", "1.3")
                .queryParam("returnType", "json")
                .build(true)
                .toUri();

        AirQualityResponseDto response = airQualityWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(AirQualityResponseDto.class)
                .block();

        Map<String, String> resultMap = new HashMap<>();
        if (response != null && response.response() != null && response.response().body() != null) {
            List<AirQualityResponseDto.Item> items = response.response().body().items();
            if (items != null && !items.isEmpty()) {
                AirQualityResponseDto.Item recentItem = items.get(0);
                resultMap.put("pm10Value", recentItem.pm10Value());
                resultMap.put("pm25Value", recentItem.pm25Value());
                resultMap.put("pm10Value24", recentItem.pm10value24());
                resultMap.put("pm25Value24", recentItem.pm25Value24());
                resultMap.put("pm10Grade1h", recentItem.pm10Grade1h());
                resultMap.put("pm25Grade1h", recentItem.pm25Grade1h());
            }
        }
        return resultMap;
    }

    private String getRecentForecastBaseTime(String baseDate, String baseTime) {
        int hour = Integer.parseInt(baseTime.substring(0, 2));
        int[] forecastHours = {2, 5, 8, 11, 14, 17, 20, 23};

        int targetHour = -1;
        for (int i = forecastHours.length - 1; i >= 0; i--) {
            if (hour >= forecastHours[i]) {
                targetHour = forecastHours[i];
                break;
            }
        }

        if (targetHour == -1) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            OffsetDateTime prevDate = OffsetDateTime.parse(baseDate + "0000",
                    DateTimeFormatter.ofPattern("yyyyMMddHHmm")).minusDays(1);
            return prevDate.format(formatter) + "2300";
        }

        return String.format("%02d00", targetHour);
    }
}
