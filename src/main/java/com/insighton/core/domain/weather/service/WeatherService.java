package com.insighton.core.domain.weather.service;

import com.insighton.core.adapter.client.external.AirQualityApiClient;
import com.insighton.core.adapter.client.external.KmaWeatherApiClient;
import com.insighton.core.domain.weather.dto.AirQualityDto;
import com.insighton.core.domain.weather.dto.CurrentWeatherDto;
import com.insighton.core.domain.weather.dto.ForecastBaseDateTime;
import com.insighton.core.domain.weather.dto.ForecastWeatherDto;
import com.insighton.core.domain.weather.dto.UltraForecastWeatherDto;
import com.insighton.core.domain.weather.dto.WeatherDataDto;
import com.insighton.core.domain.weather.exception.WeatherApiException;
import com.insighton.core.domain.weather.parser.SidoNameParser;
import com.insighton.core.domain.weather.util.WeatherCodeMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final KmaWeatherApiClient kmaWeatherApiClient;
    private final AirQualityApiClient airQualityApiClient;
    private final SidoNameParser sidoNameParser;

    public CurrentWeatherDto currentWeather(int gridX, int gridY, String baseDate, String baseTime) {
        // 기상청 초단기실황
        try {
            Map<String, String> currentMap = kmaWeatherApiClient.fetchKmaApi("/getUltraSrtNcst", gridX, gridY, baseDate,
                    baseTime, true);

            return new CurrentWeatherDto(
                    currentMap.getOrDefault("T1H", "N/A"),
                    currentMap.getOrDefault("REH", "N/A"),
                    currentMap.getOrDefault("RN1", "N/A"),
                    WeatherCodeMapper.parsePty(currentMap.get("PTY"))
            );
        } catch (Exception e) {
            throw new WeatherApiException("초단기실황 API 연동 중 오류 발생");
        }
    }

    public ForecastWeatherDto forecastWeather(int gridX, int gridY, String baseDate, String baseTime) {
        // 기상청 단기예보
        try {
            ForecastBaseDateTime fcstBaseTime = getRecentVilageFcstBaseTime(baseDate, baseTime);
            Map<String, String> forecastMap = kmaWeatherApiClient.fetchKmaApi("/getVilageFcst", gridX, gridY,
                    fcstBaseTime.baseDate(), fcstBaseTime.baseTime(), false);

            return new ForecastWeatherDto(
                    forecastMap.getOrDefault("TMP", "N/A"),
                    forecastMap.getOrDefault("TMX", "N/A"),
                    forecastMap.getOrDefault("TMN", "N/A"),
                    WeatherCodeMapper.parseSky(forecastMap.get("SKY")),
                    forecastMap.getOrDefault("POP", "N/A"),
                    WeatherCodeMapper.parsePty(forecastMap.get("PTY"))
            );
        } catch (Exception e) {
            throw new WeatherApiException("단기예보 API 연동 중 오류 발생");
        }
    }

    public UltraForecastWeatherDto ultraForecastWeather(int gridX, int gridY, String baseDate, String baseTime) {
        // 기상청 초단기예보
        try {
            ForecastBaseDateTime ultraFcstBastTime = getRecentUltraSrtFcstBaseTime(baseDate, baseTime);
            Map<String, String> ultraForecastMap = kmaWeatherApiClient.fetchKmaApi("/getUltraSrtFcst", gridX, gridY,
                    ultraFcstBastTime.baseDate(),
                    ultraFcstBastTime.baseTime(), false);

            return new UltraForecastWeatherDto(
                    ultraForecastMap.getOrDefault("RN1", "N/A")
            );
        } catch (Exception e) {
            throw new WeatherApiException("초단기예보 API 연동 중 오류 발생");
        }
    }

    public AirQualityDto airQuality(int girdX, int gridY, String sidoName, String cityName, String baseDate,
                                    String baseTime) {
        try {
            String parsedSidoName = sidoNameParser.parse(sidoName);
            Map<String, String> airMap = airQualityApiClient.fetchAirQualityData(parsedSidoName, cityName);

            return new AirQualityDto(
                    airMap.getOrDefault("pm10Value", "N/A"),
                    airMap.getOrDefault("pm25Value", "N/A"),
                    airMap.getOrDefault("pm10Value24", "N/A"),
                    airMap.getOrDefault("pm25Value24", "N/A"),
                    WeatherCodeMapper.parseAirGrade(airMap.get("pm10Grade1h")),
                    WeatherCodeMapper.parseAirGrade(airMap.get("pm25Grade1h"))
            );
        } catch (Exception e) {
            throw new WeatherApiException("미세먼지 API 연동 중 오류 발생");
        }
    }

    public WeatherDataDto fetchWeatherData(int gridX, int gridY, String sidoName, String cityName, String baseDate,
                                           String baseTime) {
        // 항목 하나가 실패해도 나머지 조회가 막히지 않도록 개별 격리 - 실패한 항목만 null로 남기고 계속 진행
        CurrentWeatherDto current = safeFetch(() -> currentWeather(gridX, gridY, baseDate, baseTime), "초단기실황");
        ForecastWeatherDto forecast = safeFetch(() -> forecastWeather(gridX, gridY, baseDate, baseTime), "단기예보");
        UltraForecastWeatherDto ultraForecast = safeFetch(() -> ultraForecastWeather(gridX, gridY, baseDate, baseTime), "초단기예보");
        AirQualityDto air = safeFetch(() -> airQuality(gridX, gridY, sidoName, cityName, baseDate, baseTime), "미세먼지");

        return new WeatherDataDto(current, forecast, ultraForecast, air);
    }

    private <T> T safeFetch(Supplier<T> fetcher, String label) {
        try {
            return fetcher.get();
        } catch (Exception e) {
            log.warn("{} 조회 실패 - 해당 항목만 null로 저장", label, e);
            return null;
        }
    }

    /**
     * 기상청 단기예보(/getVilageFcst) 기준 시각 계산 로직 - 단기예보는 02, 05, 08, 11, 14, 17, 20, 23시(3시간 간격)에 발표되며, 10분이 지난 시점에 데이터가
     * 생성됩니다. - 시스템 반영 시간을 고려해 각 발표 시각보다 10분 뒤를 기준으로 삼습니다.
     */
    private ForecastBaseDateTime getRecentVilageFcstBaseTime(String baseDate, String baseTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(baseDate, dateFormatter);

        int hour = Integer.parseInt(baseTime.substring(0, 2));
        int minute = Integer.parseInt(baseTime.substring(2, 4));

        LocalTime requestTime = LocalTime.of(hour, minute);

        // 단기예보 발표 시간 정렬 배열 (새벽 2시부터 3시간 간격)
        int[] forecastHours = {2, 5, 8, 11, 14, 17, 20, 23};
        int targetHour = 23;
        LocalDate targetDate = date;

        // 현재 시각(분 포함)이 발표시각 + 10분 보다 이전인지 확인하며 가장 최근 발표 시간을 찾음
        boolean found = false;
        for (int i = forecastHours.length - 1; i >= 0; i--) {
            LocalTime releaseTimeWithBuffer = LocalTime.of(forecastHours[i], 10);
            if (requestTime.isAfter(releaseTimeWithBuffer) || requestTime.equals(releaseTimeWithBuffer)) {
                targetHour = forecastHours[i];
                found = true;
                break;
            }
        }

        // 만약 새벽 02:10보다 이른 시간이면, 전날 밤 23시 데이터로 조회해야 함
        if (!found) {
            targetDate = date.minusDays(1);
            targetHour = 23;
        }

        return new ForecastBaseDateTime(
                targetDate.format(dateFormatter),
                String.format("%02d00", targetHour)
        );
    }

    /**
     * 기상청 초단기예보(/getUltraSrtFcst) 기준 시각 계산 로직 - 초단기예보는 매시 30분에 발표되므로, 시스템 반영 시간을 고려해 30분 전 시간을 기준으로 삼습니다.
     */
    private ForecastBaseDateTime getRecentUltraSrtFcstBaseTime(String baseDate, String baseTime) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(baseDate, dateFormatter);

        int hour = Integer.parseInt(baseTime.substring(0, 2));
        int minute = Integer.parseInt(baseTime.substring(2, 4));

        LocalTime time = LocalTime.of(hour, minute).minusMinutes(30);

        String targetDate = date.format(dateFormatter);
        String targetTime = String.format("%02d30", time.getHour());

        // 자정 이전(00시 30분 이전) 요청 시 전날 23시 30분 데이터로 보정
        if (time.getHour() == 23 && hour == 0) {
            targetDate = date.minusDays(1).format(dateFormatter);
        }

        return new ForecastBaseDateTime(targetDate, targetTime);
    }
}
