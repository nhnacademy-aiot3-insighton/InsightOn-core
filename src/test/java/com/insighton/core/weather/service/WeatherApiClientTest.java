package com.insighton.core.weather.service;

import com.insighton.core.adapter.client.external.WeatherApiClient;
import com.insighton.core.domain.weather.dto.AirQualityResponseDto;
import com.insighton.core.domain.weather.dto.KmaWeatherResponseDto;
import com.insighton.core.domain.weather.dto.WeatherDataDto;
import com.insighton.core.domain.weather.exception.WeatherApiException;
import com.insighton.core.domain.weather.parser.SidoNameParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "classpath:weather-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Disabled
class WeatherApiClientTest {

    private static final Logger log = LoggerFactory.getLogger(WeatherApiClientTest.class);

    @MockitoBean
    private RedisTemplate<String, WeatherDataDto> weatherRedisTemplate;

    @MockitoBean(name = "kmaRestClient")
    private RestClient kmaRestClient;

    @MockitoBean(name = "airQualityRestClient")
    private RestClient airQualityRestClient;

    @MockitoBean
    private SidoNameParser sidoNameParser;

    @MockitoBean
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @MockitoBean
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @MockitoBean
    private RestClient.ResponseSpec responseSpec;

    @Autowired
    private WeatherApiClient weatherApiClient;

    @Test
    @DisplayName("기상청 & 에어코리아 API 연동 응답 파싱 및 한글 정제 검증")
    @Disabled
    void fetchWeatherData_RealApiIntegration_Success() {
        // given: 광주 동구 계림동 기준 (X: 58, Y: 74)
        int gridX = 58;
        int gridY = 74;
        String sidoName = "광주";
        String cityName = "동구";

        String baseDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = LocalTime.now().minusHours(1).format(DateTimeFormatter.ofPattern("HH00"));

        // 외부 RestClient Mocking 세팅 (실제 키 호출 없이 동작)
        given(sidoNameParser.parse(sidoName)).willReturn("광주");
        given(kmaRestClient.get()).willReturn(requestHeadersUriSpec);
        given(airQualityRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(any(URI.class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);

        // 기상청 Mock 응답 세팅
        KmaWeatherResponseDto kmaMock = new KmaWeatherResponseDto(
                new KmaWeatherResponseDto.Response(
                        new KmaWeatherResponseDto.Header("00", "NORMAL_SERVICE"),
                        new KmaWeatherResponseDto.Body("JSON", new KmaWeatherResponseDto.Items(List.of(
                                new KmaWeatherResponseDto.Item(baseDate, baseTime, "T1H", baseDate, baseTime, "20.0",
                                        "20.0", gridX, gridY),
                                new KmaWeatherResponseDto.Item(baseDate, baseTime, "REH", baseDate, baseTime, "50",
                                        "50", gridX, gridY),
                                new KmaWeatherResponseDto.Item(baseDate, baseTime, "RN1", baseDate, baseTime, "0", "0",
                                        gridX, gridY),
                                new KmaWeatherResponseDto.Item(baseDate, baseTime, "PTY", baseDate, baseTime, "0", "0",
                                        gridX, gridY),
                                new KmaWeatherResponseDto.Item(baseDate, baseTime, "SKY", baseDate, baseTime, "1", "1",
                                        gridX, gridY),
                                new KmaWeatherResponseDto.Item(baseDate, baseTime, "TMX", baseDate, baseTime, "25.0",
                                        "25.0", gridX, gridY),
                                new KmaWeatherResponseDto.Item(baseDate, baseTime, "TMN", baseDate, baseTime, "15.0",
                                        "15.0", gridX, gridY),
                                new KmaWeatherResponseDto.Item(baseDate, baseTime, "POP", baseDate, baseTime, "0", "0",
                                        gridX, gridY)
                        )), 1, 100, 8)
                )
        );

        // 에어코리아 Mock 응답 세팅
        AirQualityResponseDto airMock = new AirQualityResponseDto(
                new AirQualityResponseDto.Response(
                        new AirQualityResponseDto.Header("00", "NORMAL_SERVICE"),
                        new AirQualityResponseDto.Body(List.of(
                                new AirQualityResponseDto.Item("동구", "동구", "15", "8", "15", "8", "1", "1")
                        ), 1)
                )
        );

        given(responseSpec.body(KmaWeatherResponseDto.class)).willReturn(kmaMock);
        given(responseSpec.body(AirQualityResponseDto.class)).willReturn(airMock);

        // when
        WeatherDataDto result = weatherApiClient.fetchWeatherData(
                gridX, gridY, sidoName, cityName, baseDate, baseTime
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.current()).isNotNull();
        assertThat(result.forecast()).isNotNull();
        assertThat(result.airQuality()).isNotNull();

        // 1. 상태 파싱 검증 (숫자 코드가 한글로 변경되었는지 확인)
        assertThat(result.current().precipitationType()).isEqualTo("없음"); // PTY '0' -> '없음'
        assertThat(result.forecast().skyStatus()).isEqualTo("맑음");        // SKY '1' -> '맑음'
        assertThat(result.airQuality().pm10Grade()).isEqualTo("좋음");       // Grade '1' -> '좋음'

        // 2. 콘솔 출력 로직
        log.info("====== [날씨/대기질 통합 API 연동 결과] ======");
        log.info("[초단기실황] 기온: {}℃, 습도: {}%, 1시간강수량: {}mm, 강수형태: {}",
                result.current().temp(), result.current().humidity(),
                result.current().hourlyRainFall(), result.current().precipitationType());

        log.info("[단기예보] 최고기온: {}℃, 최저기온: {}℃, 하늘상태: {}, 강수확률: {}%",
                result.forecast().maxTemp(), result.forecast().minTemp(),
                result.forecast().skyStatus(), result.forecast().rainPrecipitation());

        log.info("[대기질] 미세먼지: {}㎍/㎥ ({}), 초미세먼지: {}㎍/㎥ ({})",
                result.airQuality().pm10Value(), result.airQuality().pm10Grade(),
                result.airQuality().pm25Value(), result.airQuality().pm25Grade());
        log.info("===============================================");
    }

    @Test
    @DisplayName("잘못된 입력/응답 누락 시 N/A 또는 기본값 fallback 처리 검증")
    @Disabled
    void fetchWeatherData_FallbackToDefault_WhenDataMissing() {
        // given
        int invalidGridX = 999;
        int invalidGridY = 999;
        String invalidSido = "알수없는시";
        String invalidCity = "알수없는구";
        String baseDate = "20200101";
        String baseTime = "0000";

        given(sidoNameParser.parse(invalidSido)).willReturn("알수없는시");
        given(kmaRestClient.get()).willReturn(requestHeadersUriSpec);
        given(airQualityRestClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(any(URI.class))).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);

        given(responseSpec.body(KmaWeatherResponseDto.class)).willReturn(new KmaWeatherResponseDto(null));
        given(responseSpec.body(AirQualityResponseDto.class)).willReturn(new AirQualityResponseDto(null));

        // when
        WeatherDataDto result = weatherApiClient.fetchWeatherData(
                invalidGridX, invalidGridY, invalidSido, invalidCity, baseDate, baseTime
        );

        // then
        assertThat(result.current().temp()).isEqualTo("N/A");
        assertThat(result.forecast().skyStatus()).isEqualTo("알수없음");
        assertThat(result.airQuality().pm10Grade()).isEqualTo("정보없음");

        log.info("[Fallback 검증 성공] 수신 불가능한 데이터 N/A 처리 완료: {}", result);
    }

    @Test
    @DisplayName("외부 RestClient 예외 발생 시 WeatherApiException 던져지는지 단언")
    @Disabled
    void fetchWeatherData_ThrowsWeatherApiException_WhenApiFails() {
        // given
        given(sidoNameParser.parse(any())).willReturn("광주");
        given(kmaRestClient.get()).willThrow(new RuntimeException("외부 API 통신 장애"));

        // when & then
        assertThrows(WeatherApiException.class, () ->
                weatherApiClient.fetchWeatherData(999, 999, "오류시", "오류구", "20200101", "0000")
        );
    }
}