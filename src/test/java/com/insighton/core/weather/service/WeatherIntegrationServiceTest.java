package com.insighton.core.weather.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.insighton.core.weather.dto.WeatherDataDto;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "weather.api.kma-base-url=http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0",
        "weather.api.air-base-url=http://apis.data.go.kr/B552584/ArpltnInforInqireSvc",
        "weather.api.kma-key=API KEY",
        // 실제 사용하는 API KEY 입력 (또는 mock key)
        "weather.api.air-key=API KEY"
        // 실제 사용하는 API KEY 입력 (또는 mock key)
})
class WeatherIntegrationServiceTest {

    private static final Logger log = LoggerFactory.getLogger(WeatherIntegrationServiceTest.class);

//    @MockBean
//    private RedisTemplate<String, WeatherDataDto> weatherRedisTemplate;

    @Autowired
    private WeatherIntegrationService weatherIntegrationService;

    @Test
    @DisplayName("기상청 & 에어코리아 API 실제 호출 연동 검증 및 콘솔 출력")
    void fetchWeatherData_RealApiIntegration_Success() {
        // given: 광주 동구 계림동 기준 (X: 58, Y: 74)
        int gridX = 58;
        int gridY = 74;
        String sidoName = "광주";
        String cityName = "동구";

        String baseDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // API 정각 데이터 수신 오차 감안하여 현재 시간 - 1시간 전으로 호출
        String baseTime = LocalTime.now().minusHours(1).format(DateTimeFormatter.ofPattern("HH00"));

        // when
        WeatherDataDto result = weatherIntegrationService.fetchWeatherData(
                gridX, gridY, sidoName, cityName, baseDate, baseTime
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.current()).isNotNull();
        assertThat(result.forecast()).isNotNull();
        assertThat(result.airQuality()).isNotNull();

        // 1. 상태 파싱 검증 (한글 변환 여부)
        assertThat(result.current().precipitationType()).isNotIn("0", "1", "2", "3", "4"); // 숫자가 아니어야 함
        assertThat(result.forecast().skyStatus()).isNotIn("1", "3", "4"); // 숫자가 아니어야 함

        // 2. 콘솔 출력 로직 포함
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
    void fetchWeatherData_FallbackToDefault_WhenDataMissing() {
        // given: 존재하지 않는 지역 입력
        int invalidGridX = 999;
        int invalidGridY = 999;
        String invalidSido = "알수없는시";
        String invalidCity = "알수없는구";
        String baseDate = "20200101";
        String baseTime = "0000";

        // when & then
        // 예외가 발생하거나 혹은 N/A 값으로 정제되는지 확인
        try {
            WeatherDataDto result = weatherIntegrationService.fetchWeatherData(
                    invalidGridX, invalidGridY, invalidSido, invalidCity, baseDate, baseTime
            );

            // 데이터가 비어있을 경우 N/A로 채워져야 함
            assertThat(result.current().temp()).isEqualTo("N/A");
            assertThat(result.forecast().skyStatus()).isEqualTo("알수없음");
            assertThat(result.airQuality().pm10Grade()).isEqualTo("정보없음");

            log.info("[Fallback 검증 성공] 수신 불가능한 데이터 N/A 처리 완료: {}", result);
        } catch (Exception e) {
            log.info("[Fallback 검증] 외부 API 연동 실패 예외가 정상 발생함: {}", e.getMessage());
        }
    }
}