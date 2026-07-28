package com.insighton.core.weather.service;

import com.insighton.core.exception.WeatherApiException;
import com.insighton.core.weather.dto.AirQualityResponseDto;
import com.insighton.core.weather.dto.KmaWeatherResponseDto;
import com.insighton.core.weather.dto.WeatherDataDto;
import com.insighton.core.weather.parser.SidoNameParser;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WeatherIntegrationService {

    private final WebClient kmaWebClient;
    private final WebClient airQualityWebClient;
    private final SidoNameParser sidoNameParser;

    @Value("${weather.api.kma-key}")
    private String kmaApiKey;

    @Value("${weather.api.air-key}")
    private String airApiKey;

    public WeatherIntegrationService(
            @Qualifier("kmaWebClient") WebClient kmaWebClient,
            @Qualifier("airQualityWebClient") WebClient airQualityWebClient,
            SidoNameParser sidoNameParser) {
        this.kmaWebClient = kmaWebClient;
        this.airQualityWebClient = airQualityWebClient;
        this.sidoNameParser = sidoNameParser;
    }

    public WeatherDataDto fetchWeatherData(int gridX, int gridY, String sidoName, String cityName, String baseDate,
                                           String baseTime) {
        String parsedSidoName = sidoNameParser.parse(sidoName);
        try {
            // 기상청 초단기실황
            Map<String, String> currentMap = fetchKmaApi("/getUltraSrtNcst", gridX, gridY, baseDate, baseTime, true);

            // 기상청 단기예보
            String fcstBaseTime = getRecentForecastBaseTime(baseDate, baseTime);
            Map<String, String> forecastMap = fetchKmaApi("/getVilageFcst", gridX, gridY, baseDate, fcstBaseTime,
                    false);

            // 에어코리아 미세먼지 정보
            Map<String, String> airMap = fetchAirQualityData(parsedSidoName, cityName);

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
        String url = String.format(
                "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0%s?serviceKey=%s&pageNo=1&numOfRows=1000&dataType=JSON&base_date=%s&base_time=%s&nx=%d&ny=%d",
                endpoint, kmaApiKey, baseDate, baseTime, nx, ny
        );

        KmaWeatherResponseDto response = kmaWebClient.get()
                .uri(URI.create(url))
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

    private Map<String, String> fetchAirQualityData(String sidoName, String cityName) {
        String encodedSidoName = URLEncoder.encode(sidoName, StandardCharsets.UTF_8);
        String url = String.format(
                "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty?serviceKey=%s&sidoName=%s&pageNo=1&numOfRows=100&ver=1.3&returnType=json",
                airApiKey, encodedSidoName
        );

        AirQualityResponseDto response = airQualityWebClient.get()
                .uri(URI.create(url))
                .retrieve()
                .bodyToMono(AirQualityResponseDto.class)
                .block();

        Map<String, String> resultMap = new HashMap<>();
        if (response != null && response.response() != null && response.response().body() != null) {
            List<AirQualityResponseDto.Item> items = response.response().body().items();
            if (items != null && !items.isEmpty()) {
                AirQualityResponseDto.Item selectedItem = items.stream()
                        .filter(item -> item.cityName() != null && item.cityName().equals(cityName))
                        .findFirst()
                        .orElseGet(() -> items.stream()
                                .filter(item -> item.stationName() != null && item.stationName().contains(cityName))
                                .findFirst()
                                .orElse(items.get(0)));

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