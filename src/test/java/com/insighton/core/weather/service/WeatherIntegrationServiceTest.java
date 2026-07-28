package com.insighton.core.weather.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.insighton.core.weather.dto.WeatherDataDto;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
class WeatherIntegrationServiceTest {

    @Autowired
    private WeatherIntegrationService weatherIntegrationService;

    @Test
    @DisplayName("실제 날씨(초단기실황+단기예보) 및 미세먼지 API 호출 통합 테스트")
    void fetchWeatherData_Success() {
        int gridX = 48;
        int gridY = 59;

        String sidoName = "전남광주";
        String cityName = "진도군";

        LocalDateTime now = LocalDateTime.now().minusHours(1);
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = now.format(DateTimeFormatter.ofPattern("HH00"));

        WeatherDataDto result = weatherIntegrationService.fetchWeatherData(
                gridX, gridY, sidoName, cityName, baseDate, baseTime);

        System.out.println("\n==========================================");
        System.out.println("     [API 통합 연동 테스트 결과 출력]");
        System.out.println("==========================================");
        System.out.println("[기상청 초단기실황 날씨 데이터]");
        System.out.println(" - 현재 기온 (T1H)    : " + result.temp() + "°C");
        System.out.println(" - 습도 (REH)        : " + result.humidity() + "%");
        System.out.println(" - 1시간 강수량 (RN1): " + result.hourlyRainfall() + "mm");
        System.out.println(" - 강수형태 (PTY)    : " + result.precipitationType());
        System.out.println("------------------------------------------");
        System.out.println("[기상청 단기예보 데이터]");
        System.out.println(" - 최고 기온 (TMX)   : " + result.maxTemp() + "°C");
        System.out.println(" - 최저 기온 (TMN)   : " + result.minTemp() + "°C");
        System.out.println(" - 하늘 상태 (SKY)   : " + result.skyStatus());
        System.out.println(" - 강수 확률 (POP)   : " + result.rainPrecipitation() + "%");
        System.out.println("------------------------------------------");
        System.out.println("[에어코리아 미세먼지 데이터]");
        System.out.println(" - 미세먼지 (PM10)   : " + result.pm10Value() + " ㎛/㎥");
        System.out.println(" - 초미세먼지 (PM2.5): " + result.pm25Value() + " ㎛/㎥");
        System.out.println(" - PM10 24시간 예측  : " + result.pm10Value24());
        System.out.println(" - PM2.5 24시간 예측 : " + result.pm25Value24());
        System.out.println(" - PM10 1시간 등급   : " + result.pm10Grade1h());
        System.out.println(" - PM2.5 1시간 등급  : " + result.pm25Grade1h());
        System.out.println("==========================================\n");

        // --- 2. 핵심 데이터 검증 (Assertion) ---
        assertThat(result).isNotNull();

        // [기상청 초단기실황 검증]
        assertThat(result.temp()).isNotEqualTo("N/A");
        assertThat(result.humidity()).isNotEqualTo("N/A");

        // [기상청 단기예보 검증]
        assertThat(result.skyStatus()).isNotEqualTo("N/A");
        assertThat(result.rainPrecipitation()).isNotEqualTo("N/A");

        // [에어코리아 미세먼지 검증]
        assertThat(result.pm10Value()).isNotEqualTo("N/A");
        assertThat(result.pm25Value()).isNotEqualTo("N/A");
    }
}