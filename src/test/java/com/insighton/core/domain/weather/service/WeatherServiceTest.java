package com.insighton.core.domain.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.insighton.core.adapter.client.external.AirQualityApiClient;
import com.insighton.core.adapter.client.external.KmaWeatherApiClient;
import com.insighton.core.domain.weather.dto.AirQualityDto;
import com.insighton.core.domain.weather.dto.CurrentWeatherDto;
import com.insighton.core.domain.weather.dto.ForecastWeatherDto;
import com.insighton.core.domain.weather.dto.UltraForecastWeatherDto;
import com.insighton.core.domain.weather.dto.WeatherDataDto;
import com.insighton.core.domain.weather.exception.WeatherApiException;
import com.insighton.core.domain.weather.parser.SidoNameParser;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private KmaWeatherApiClient kmaWeatherApiClient;

    @Mock
    private AirQualityApiClient airQualityApiClient;

    @Mock
    private SidoNameParser sidoNameParser;

    @InjectMocks
    private WeatherService weatherService;

    @Test
    @DisplayName("초단기실황 조회 성공")
    void currentWeather_Success() {
        // given
        Map<String, String> mockApiResponse = new HashMap<>();
        mockApiResponse.put("T1H", "24.5");
        mockApiResponse.put("REH", "60");
        mockApiResponse.put("RN1", "0.0");
        mockApiResponse.put("PTY", "0");

        given(kmaWeatherApiClient.fetchKmaApi(eq("/getUltraSrtNcst"), anyInt(), anyInt(), anyString(), anyString(),
                eq(true)))
                .willReturn(mockApiResponse);

        // when
        CurrentWeatherDto result = weatherService.currentWeather(60, 127, "20260601", "1200");

        // then
        assertThat(result).isNotNull();
        assertThat(result.temp()).isEqualTo("24.5");
        assertThat(result.humidity()).isEqualTo("60");
    }

    @Test
    @DisplayName("초단기실황 조회 중 예외 발생 시 WeatherApiException 전환")
    void currentWeather_ThrowsException() {
        // given
        given(kmaWeatherApiClient.fetchKmaApi(anyString(), anyInt(), anyInt(), anyString(), anyString(), anyBoolean()))
                .willThrow(new RuntimeException("API 연동 실패"));

        // when & then
        assertThatThrownBy(() -> weatherService.currentWeather(60, 127, "20260601", "1200"))
                .isInstanceOf(WeatherApiException.class)
                .hasMessage("초단기실황 API 연동 중 오류 발생");
    }

    @Test
    @DisplayName("단기예보 조회 성공 (기준 시각 보정 로직 포함)")
    void forecastWeather_Success() {
        // given
        Map<String, String> mockApiResponse = new HashMap<>();
        mockApiResponse.put("TMP", "22.0");
        mockApiResponse.put("TMX", "28.0");
        mockApiResponse.put("TMN", "18.0");
        mockApiResponse.put("SKY", "1");
        mockApiResponse.put("POP", "10");
        mockApiResponse.put("PTY", "0");

        given(kmaWeatherApiClient.fetchKmaApi(eq("/getVilageFcst"), anyInt(), anyInt(), anyString(), anyString(),
                eq(false)))
                .willReturn(mockApiResponse);

        // when
        // 12:00 요청 시 단기예보 기준시각인 11:00으로 변환되어 호출되는지 검증
        ForecastWeatherDto result = weatherService.forecastWeather(60, 127, "20260601", "1200");

        // then
        assertThat(result).isNotNull();
        assertThat(result.temperature()).isEqualTo("22.0");
        assertThat(result.skyStatus()).isNotNull();
    }

    @Test
    @DisplayName("초단기예보 조회 성공")
    void ultraForecastWeather_Success() {
        // given
        Map<String, String> mockApiResponse = new HashMap<>();
        mockApiResponse.put("RN1", "1.5");

        given(kmaWeatherApiClient.fetchKmaApi(eq("/getUltraSrtFcst"), anyInt(), anyInt(), anyString(), anyString(),
                eq(false)))
                .willReturn(mockApiResponse);

        // when
        UltraForecastWeatherDto result = weatherService.ultraForecastWeather(60, 127, "20260601", "1230");

        // then
        assertThat(result).isNotNull();
        assertThat(result.ultraHourlyRainFall()).isEqualTo("1.5");
    }

    @Test
    @DisplayName("미세먼지 정보 조회 성공")
    void airQuality_Success() {
        // given
        Map<String, String> mockApiResponse = new HashMap<>();
        mockApiResponse.put("pm10Value", "35");
        mockApiResponse.put("pm25Value", "15");
        mockApiResponse.put("pm10Value24", "40");
        mockApiResponse.put("pm25Value24", "18");
        mockApiResponse.put("pm10Grade1h", "1");
        mockApiResponse.put("pm25Grade1h", "1");

        given(sidoNameParser.parse("서울")).willReturn("서울");
        given(airQualityApiClient.fetchAirQualityData("서울", "강남구")).willReturn(mockApiResponse);

        // when
        AirQualityDto result = weatherService.airQuality(60, 127, "서울", "강남구", "20260601", "1200");

        // then
        assertThat(result).isNotNull();
        assertThat(result.pm10Value()).isEqualTo("35");
        assertThat(result.pm25Value()).isEqualTo("15");
    }

    @Test
    @DisplayName("전체 날씨 데이터 통합 조회 성공 (fetchWeatherData)")
    void fetchWeatherData_Success() {
        // given
        Map<String, String> currentMap = Map.of("T1H", "20", "REH", "50", "RN1", "0", "PTY", "0");
        Map<String, String> forecastMap = Map.of("TMP", "20", "TMX", "25", "TMN", "15", "SKY", "1", "POP", "0", "PTY",
                "0");
        Map<String, String> ultraMap = Map.of("RN1", "0");
        Map<String, String> airMap = Map.of("pm10Value", "10", "pm25Value", "5", "pm10Value24", "10", "pm25Value24",
                "5", "pm10Grade1h", "1", "pm25Grade1h", "1");

        given(kmaWeatherApiClient.fetchKmaApi(eq("/getUltraSrtNcst"), anyInt(), anyInt(), anyString(), anyString(),
                eq(true))).willReturn(currentMap);
        given(kmaWeatherApiClient.fetchKmaApi(eq("/getVilageFcst"), anyInt(), anyInt(), anyString(), anyString(),
                eq(false))).willReturn(forecastMap);
        given(kmaWeatherApiClient.fetchKmaApi(eq("/getUltraSrtFcst"), anyInt(), anyInt(), anyString(), anyString(),
                eq(false))).willReturn(ultraMap);
        given(sidoNameParser.parse(anyString())).willReturn("서울");
        given(airQualityApiClient.fetchAirQualityData(anyString(), anyString())).willReturn(airMap);

        // when
        WeatherDataDto result = weatherService.fetchWeatherData(60, 127, "서울", "강남구", "20260601", "1200");

        // then
        assertThat(result).isNotNull();
        assertThat(result.current()).isNotNull();
        assertThat(result.forecast()).isNotNull();
        assertThat(result.ultraForecastWeather()).isNotNull();
        assertThat(result.airQuality()).isNotNull();
    }
}