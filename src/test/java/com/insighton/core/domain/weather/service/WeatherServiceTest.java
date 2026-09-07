package com.insighton.core.domain.weather.service;

import com.insighton.core.adapter.client.external.AirQualityApiClient;
import com.insighton.core.adapter.client.external.KmaWeatherApiClient;
import com.insighton.core.domain.weather.dto.AirQualityDto;
import com.insighton.core.domain.weather.dto.CurrentWeatherDto;
import com.insighton.core.domain.weather.dto.ForecastWeatherDto;
import com.insighton.core.domain.weather.dto.MidTermTemperatureDto;
import com.insighton.core.domain.weather.dto.MidTermTemperatureResponseDto;
import com.insighton.core.domain.weather.dto.UltraForecastWeatherDto;
import com.insighton.core.domain.weather.dto.WeatherDataDto;
import com.insighton.core.domain.weather.exception.WeatherApiException;
import com.insighton.core.domain.weather.parser.MidTermRegionCodeMapper;
import com.insighton.core.domain.weather.parser.SidoNameParser;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private KmaWeatherApiClient kmaWeatherApiClient;

    @Mock
    private AirQualityApiClient airQualityApiClient;

    @Mock
    private SidoNameParser sidoNameParser;

    @Mock
    private MidTermRegionCodeMapper midTermRegionCodeMapper;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(kmaWeatherApiClient, airQualityApiClient, sidoNameParser,
                midTermRegionCodeMapper);
    }

    @Nested
    class CurrentWeatherTest {

        @Test
        void currentWeather_초단기실황_응답을_DTO로_변환한다() {
            given(kmaWeatherApiClient.fetchKmaApi("/getUltraSrtNcst", 60, 127, "20260907", "1200", true))
                    .willReturn(Map.of("T1H", "23.5", "REH", "55", "RN1", "0", "PTY", "1"));

            CurrentWeatherDto result = weatherService.currentWeather(60, 127, "20260907", "1200");

            assertThat(result.temp()).isEqualTo("23.5");
            assertThat(result.humidity()).isEqualTo("55");
            assertThat(result.hourlyRainFall()).isEqualTo("0");
            assertThat(result.precipitationType()).isEqualTo("비");
        }

        @Test
        void currentWeather_클라이언트_예외는_WeatherApiException으로_변환된다() {
            given(kmaWeatherApiClient.fetchKmaApi("/getUltraSrtNcst", 60, 127, "20260907", "1200", true))
                    .willThrow(new RuntimeException("connection reset"));

            assertThatThrownBy(() -> weatherService.currentWeather(60, 127, "20260907", "1200"))
                    .isInstanceOf(WeatherApiException.class);
        }
    }

    @Nested
    class ForecastWeatherTest {

        @Test
        void forecastWeather_요청시각_기준_가장_최근_발표시각으로_조회한다() {
            // 14:15 요청 -> 14:10 발표분(14:00) 사용
            given(kmaWeatherApiClient.fetchKmaApi("/getVilageFcst", 60, 127, "20260907", "1400", false))
                    .willReturn(Map.of("TMP", "24", "TMX", "27", "TMN", "18", "SKY", "3", "POP", "30", "PTY", "0"));

            ForecastWeatherDto result = weatherService.forecastWeather(60, 127, "20260907", "1415");

            assertThat(result.temperature()).isEqualTo("24");
            assertThat(result.maxTemp()).isEqualTo("27");
            assertThat(result.minTemp()).isEqualTo("18");
            assertThat(result.skyStatus()).isEqualTo("구름많음");
            assertThat(result.rainPrecipitation()).isEqualTo("30");
            assertThat(result.forecastPrecipitationType()).isEqualTo("없음");
        }

        @Test
        void forecastWeather_새벽_02시10분_이전이면_전날_23시_발표분을_사용한다() {
            given(kmaWeatherApiClient.fetchKmaApi("/getVilageFcst", 60, 127, "20260906", "2300", false))
                    .willReturn(Map.of("TMP", "10", "TMX", "N/A", "TMN", "N/A", "SKY", "1", "POP", "0", "PTY", "0"));

            ForecastWeatherDto result = weatherService.forecastWeather(60, 127, "20260907", "0105");

            assertThat(result.temperature()).isEqualTo("10");
        }

        @Test
        void forecastWeather_클라이언트_예외는_WeatherApiException으로_변환된다() {
            given(kmaWeatherApiClient.fetchKmaApi(eq("/getVilageFcst"), anyInt(), anyInt(), anyString(), anyString(),
                    eq(false)))
                    .willThrow(new RuntimeException("timeout"));

            assertThatThrownBy(() -> weatherService.forecastWeather(60, 127, "20260907", "1415"))
                    .isInstanceOf(WeatherApiException.class);
        }
    }

    @Nested
    class UltraForecastWeatherTest {

        @Test
        void ultraForecastWeather_요청시각_30분_전_발표분을_사용한다() {
            given(kmaWeatherApiClient.fetchKmaApi("/getUltraSrtFcst", 60, 127, "20260907", "1330", false))
                    .willReturn(Map.of("RN1", "1"));

            UltraForecastWeatherDto result = weatherService.ultraForecastWeather(60, 127, "20260907", "1415");

            assertThat(result.ultraHourlyRainFall()).isEqualTo("1");
        }

        @Test
        void ultraForecastWeather_자정_직후_요청이면_전날_23시30분_발표분을_사용한다() {
            given(kmaWeatherApiClient.fetchKmaApi("/getUltraSrtFcst", 60, 127, "20260906", "2330", false))
                    .willReturn(Map.of("RN1", "0"));

            UltraForecastWeatherDto result = weatherService.ultraForecastWeather(60, 127, "20260907", "0010");

            assertThat(result.ultraHourlyRainFall()).isEqualTo("0");
        }

        @Test
        void ultraForecastWeather_클라이언트_예외는_WeatherApiException으로_변환된다() {
            given(kmaWeatherApiClient.fetchKmaApi(eq("/getUltraSrtFcst"), anyInt(), anyInt(), anyString(),
                    anyString(), eq(false)))
                    .willThrow(new RuntimeException("timeout"));

            assertThatThrownBy(() -> weatherService.ultraForecastWeather(60, 127, "20260907", "1415"))
                    .isInstanceOf(WeatherApiException.class);
        }
    }

    @Nested
    class AirQualityTest {

        @Test
        void airQuality_시도명을_파싱한_뒤_응답을_DTO로_변환한다() {
            given(sidoNameParser.parse("서울특별시")).willReturn("서울");
            given(airQualityApiClient.fetchAirQualityData("서울", "강남구"))
                    .willReturn(Map.of(
                            "pm10Value", "30", "pm25Value", "15",
                            "pm10Value24", "28", "pm25Value24", "14",
                            "pm10Grade1h", "2", "pm25Grade1h", "1"));

            AirQualityDto result = weatherService.airQuality(60, 127, "서울특별시", "강남구", "20260907", "1200");

            assertThat(result.pm10Value()).isEqualTo("30");
            assertThat(result.pm25Value()).isEqualTo("15");
            assertThat(result.pm10Grade()).isEqualTo("보통");
            assertThat(result.pm25Grade()).isEqualTo("좋음");
        }

        @Test
        void airQuality_클라이언트_예외는_WeatherApiException으로_변환된다() {
            given(sidoNameParser.parse(anyString())).willReturn("서울");
            given(airQualityApiClient.fetchAirQualityData(anyString(), anyString()))
                    .willThrow(new RuntimeException("api down"));

            assertThatThrownBy(() -> weatherService.airQuality(60, 127, "서울특별시", "강남구", "20260907", "1200"))
                    .isInstanceOf(WeatherApiException.class);
        }
    }

    @Nested
    class MidTermTemperatureTest {

        @Test
        void resolveMidTermRegId_파싱된_시도명을_regId로_변환한다() {
            given(sidoNameParser.parse("서울특별시")).willReturn("서울");
            given(midTermRegionCodeMapper.toRegId("서울")).willReturn("11B10101");

            String regId = weatherService.resolveMidTermRegId("서울특별시");

            assertThat(regId).isEqualTo("11B10101");
        }

        @Test
        void midTermTemperature_18시10분_이후_요청이면_당일_18시_발표분을_사용하고_유효값만_평균낸다() {
            given(sidoNameParser.parse("서울특별시")).willReturn("서울");
            given(midTermRegionCodeMapper.toRegId("서울")).willReturn("11B10101");
            given(kmaWeatherApiClient.fetchMidTermTemperature("11B10101", "202609071800"))
                    .willReturn(new MidTermTemperatureResponseDto.Item(
                            "11B10101",
                            "10", "20", "11", "-", "12", "22", "13", "",
                            "14", "24", "15", "25", "16", "26"));

            MidTermTemperatureDto result = weatherService.midTermTemperature("서울특별시", "20260907", "1815");

            // taMax: 20,22,24,25,26 (유효 5개) -> 평균 23.4 / taMin: 10,11,12,13,14,15,16 (유효 7개) -> 평균 13.0
            assertThat(result.avgMaxTemp()).isEqualTo("23.4");
            assertThat(result.avgMinTemp()).isEqualTo("13.0");
        }

        @Test
        void midTermTemperature_06시10분_이전_요청이면_전날_18시_발표분을_사용한다() {
            given(sidoNameParser.parse("서울특별시")).willReturn("서울");
            given(midTermRegionCodeMapper.toRegId("서울")).willReturn("11B10101");
            given(kmaWeatherApiClient.fetchMidTermTemperature("11B10101", "202609061800"))
                    .willReturn(new MidTermTemperatureResponseDto.Item(
                            "11B10101",
                            "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-"));

            MidTermTemperatureDto result = weatherService.midTermTemperature("서울특별시", "20260907", "0300");

            assertThat(result.avgMaxTemp()).isEqualTo("N/A");
            assertThat(result.avgMinTemp()).isEqualTo("N/A");
        }

        @Test
        void midTermTemperature_클라이언트_예외는_WeatherApiException으로_변환된다() {
            given(sidoNameParser.parse(anyString())).willReturn("서울");
            given(midTermRegionCodeMapper.toRegId(anyString())).willReturn("11B10101");
            given(kmaWeatherApiClient.fetchMidTermTemperature(anyString(), anyString()))
                    .willThrow(new WeatherApiException("기상청 중기기온 API 응답에 데이터가 없습니다."));

            assertThatThrownBy(() -> weatherService.midTermTemperature("서울특별시", "20260907", "1815"))
                    .isInstanceOf(WeatherApiException.class);
        }
    }

    @Nested
    class FetchWeatherDataTest {

        @Test
        void fetchWeatherData_네_항목을_모두_조회해_하나의_DTO로_합친다() {
            given(kmaWeatherApiClient.fetchKmaApi(eq("/getUltraSrtNcst"), anyInt(), anyInt(), anyString(),
                    anyString(), eq(true)))
                    .willReturn(Map.of("T1H", "23", "REH", "50", "RN1", "0", "PTY", "0"));
            given(kmaWeatherApiClient.fetchKmaApi(eq("/getVilageFcst"), anyInt(), anyInt(), anyString(), anyString(),
                    eq(false)))
                    .willReturn(Map.of("TMP", "24", "TMX", "27", "TMN", "18", "SKY", "1", "POP", "10", "PTY", "0"));
            given(kmaWeatherApiClient.fetchKmaApi(eq("/getUltraSrtFcst"), anyInt(), anyInt(), anyString(),
                    anyString(), eq(false)))
                    .willReturn(Map.of("RN1", "0"));
            given(sidoNameParser.parse(anyString())).willReturn("서울");
            given(airQualityApiClient.fetchAirQualityData(anyString(), anyString()))
                    .willReturn(Map.of("pm10Value", "30", "pm25Value", "15", "pm10Value24", "28",
                            "pm25Value24", "14", "pm10Grade1h", "1", "pm25Grade1h", "1"));

            WeatherDataDto result = weatherService.fetchWeatherData(60, 127, "서울특별시", "강남구", "20260907", "1200");

            assertThat(result.current()).isNotNull();
            assertThat(result.forecast()).isNotNull();
            assertThat(result.ultraForecastWeather()).isNotNull();
            assertThat(result.airQuality()).isNotNull();
            assertThat(result.midTermTemperature()).isNull();
        }

        @Test
        void fetchWeatherData_일부_항목이_실패해도_해당_필드만_null이_되고_나머지는_유지된다() {
            given(kmaWeatherApiClient.fetchKmaApi(eq("/getUltraSrtNcst"), anyInt(), anyInt(), anyString(),
                    anyString(), eq(true)))
                    .willThrow(new RuntimeException("초단기실황 API 장애"));
            given(kmaWeatherApiClient.fetchKmaApi(eq("/getVilageFcst"), anyInt(), anyInt(), anyString(), anyString(),
                    eq(false)))
                    .willReturn(Map.of("TMP", "24", "TMX", "27", "TMN", "18", "SKY", "1", "POP", "10", "PTY", "0"));
            given(kmaWeatherApiClient.fetchKmaApi(eq("/getUltraSrtFcst"), anyInt(), anyInt(), anyString(),
                    anyString(), eq(false)))
                    .willReturn(Map.of("RN1", "0"));
            given(sidoNameParser.parse(anyString())).willReturn("서울");
            given(airQualityApiClient.fetchAirQualityData(anyString(), anyString()))
                    .willReturn(Map.of("pm10Value", "30", "pm25Value", "15", "pm10Value24", "28",
                            "pm25Value24", "14", "pm10Grade1h", "1", "pm25Grade1h", "1"));

            WeatherDataDto result = weatherService.fetchWeatherData(60, 127, "서울특별시", "강남구", "20260907", "1200");

            assertThat(result.current()).isNull();
            assertThat(result.forecast()).isNotNull();
            assertThat(result.ultraForecastWeather()).isNotNull();
            assertThat(result.airQuality()).isNotNull();
        }
    }
}
