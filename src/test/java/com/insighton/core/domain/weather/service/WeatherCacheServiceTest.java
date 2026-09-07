package com.insighton.core.domain.weather.service;

import com.insighton.core.domain.weather.dto.AirQualityDto;
import com.insighton.core.domain.weather.dto.CurrentWeatherDto;
import com.insighton.core.domain.weather.dto.ForecastWeatherDto;
import com.insighton.core.domain.weather.dto.MidTermTemperatureDto;
import com.insighton.core.domain.weather.dto.UltraForecastWeatherDto;
import com.insighton.core.domain.weather.dto.WeatherDataDto;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WeatherCacheServiceTest {

    private static final int GRID_X = 60;
    private static final int GRID_Y = 127;
    private static final String SIDO_NAME = "서울특별시";
    private static final String CITY_NAME = "강남구";
    private static final String BASE_DATE = "20260907";
    private static final String BASE_TIME = "1200";
    private static final String CACHE_KEY = "weather:grid:60:127";
    private static final String LOCK_KEY = CACHE_KEY + ":lock";

    @Mock
    private RedisTemplate<String, WeatherDataDto> weatherRedisTemplate;
    @Mock
    private ValueOperations<String, WeatherDataDto> weatherValueOps;

    @Mock
    private RedisTemplate<String, MidTermTemperatureDto> midTermTemperatureRedisTemplate;
    @Mock
    private ValueOperations<String, MidTermTemperatureDto> midTermValueOps;

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> stringValueOps;

    @Mock
    private WeatherService weatherService;

    private WeatherCacheService weatherCacheService;

    @BeforeEach
    void setUp() {
        weatherCacheService = new WeatherCacheService(
                weatherRedisTemplate, midTermTemperatureRedisTemplate, stringRedisTemplate, weatherService);
    }

    private WeatherDataDto sampleWeatherData(MidTermTemperatureDto midTerm) {
        return new WeatherDataDto(
                new CurrentWeatherDto("23", "50", "0", "없음"),
                new ForecastWeatherDto("24", "27", "18", "맑음", "10", "없음"),
                new UltraForecastWeatherDto("0"),
                new AirQualityDto("30", "15", "28", "14", "보통", "좋음"),
                midTerm
        );
    }

    // ---------------------------------------------------------------
    // getWeatherDate
    // ---------------------------------------------------------------

    @Test
    void getWeatherDate_캐시히트면_외부API_호출없이_중기기온만_합쳐서_반환한다() {
        WeatherDataDto cached = sampleWeatherData(null);
        MidTermTemperatureDto cachedMidTerm = new MidTermTemperatureDto("20.0", "10.0");

        given(weatherRedisTemplate.opsForValue()).willReturn(weatherValueOps);
        given(weatherValueOps.get(CACHE_KEY)).willReturn(cached);

        given(weatherService.resolveMidTermRegId(SIDO_NAME)).willReturn("11B10101");
        given(midTermTemperatureRedisTemplate.opsForValue()).willReturn(midTermValueOps);
        given(midTermValueOps.get("weather:midterm:11B10101")).willReturn(cachedMidTerm);

        WeatherDataDto result = weatherCacheService.getWeatherDate(GRID_X, GRID_Y, SIDO_NAME, CITY_NAME, BASE_DATE,
                BASE_TIME);

        assertThat(result.current()).isEqualTo(cached.current());
        assertThat(result.midTermTemperature()).isEqualTo(cachedMidTerm);

        verify(weatherService, never()).fetchWeatherData(anyInt(), anyInt(), anyString(),
                anyString(), anyString(), anyString());
        verify(weatherService, never()).midTermTemperature(anyString(), anyString(), anyString());
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void getWeatherDate_캐시미스면_락을_획득해_외부API로_적재하고_캐시에_저장한다() {
        given(weatherRedisTemplate.opsForValue()).willReturn(weatherValueOps);
        given(weatherValueOps.get(CACHE_KEY)).willReturn(null);

        given(stringRedisTemplate.opsForValue()).willReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class))).willReturn(true);

        WeatherDataDto fresh = sampleWeatherData(null);
        given(weatherService.fetchWeatherData(GRID_X, GRID_Y, SIDO_NAME, CITY_NAME, BASE_DATE, BASE_TIME))
                .willReturn(fresh);

        given(weatherService.resolveMidTermRegId(SIDO_NAME)).willReturn("11B10101");
        given(midTermTemperatureRedisTemplate.opsForValue()).willReturn(midTermValueOps);
        given(midTermValueOps.get("weather:midterm:11B10101")).willReturn(null);
        MidTermTemperatureDto freshMidTerm = new MidTermTemperatureDto("21.0", "11.0");
        given(weatherService.midTermTemperature(SIDO_NAME, BASE_DATE, BASE_TIME)).willReturn(freshMidTerm);

        WeatherDataDto result = weatherCacheService.getWeatherDate(GRID_X, GRID_Y, SIDO_NAME, CITY_NAME, BASE_DATE,
                BASE_TIME);

        assertThat(result.current()).isEqualTo(fresh.current());
        assertThat(result.midTermTemperature()).isEqualTo(freshMidTerm);

        verify(weatherValueOps).set(eq(CACHE_KEY), eq(fresh), any(Duration.class));
        verify(midTermValueOps).set(eq("weather:midterm:11B10101"), eq(freshMidTerm), any(Duration.class));
        verify(stringRedisTemplate).execute(any(RedisScript.class), eq(List.of(LOCK_KEY)), any());
    }

    @Test
    void getWeatherDate_락_선점에_실패하면_대기하다가_다른_스레드가_채운_캐시를_반환한다() {
        given(weatherRedisTemplate.opsForValue()).willReturn(weatherValueOps);
        WeatherDataDto filledByOther = sampleWeatherData(null);
        given(weatherValueOps.get(CACHE_KEY)).willReturn(null, filledByOther);

        given(stringRedisTemplate.opsForValue()).willReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class))).willReturn(false);

        given(weatherService.resolveMidTermRegId(SIDO_NAME)).willReturn("11B10101");
        given(midTermTemperatureRedisTemplate.opsForValue()).willReturn(midTermValueOps);
        MidTermTemperatureDto cachedMidTerm = new MidTermTemperatureDto("20.0", "10.0");
        given(midTermValueOps.get("weather:midterm:11B10101")).willReturn(cachedMidTerm);

        WeatherDataDto result = weatherCacheService.getWeatherDate(GRID_X, GRID_Y, SIDO_NAME, CITY_NAME, BASE_DATE,
                BASE_TIME);

        assertThat(result.current()).isEqualTo(filledByOther.current());
        verify(weatherService, never()).fetchWeatherData(anyInt(), anyInt(), anyString(),
                anyString(), anyString(), anyString());
        verify(stringValueOps, times(1)).setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class));
    }

    @Test
    void getWeatherDate_중기기온_조회가_실패해도_전체_흐름은_실패하지_않고_null로_채워진다() {
        WeatherDataDto cached = sampleWeatherData(null);
        given(weatherRedisTemplate.opsForValue()).willReturn(weatherValueOps);
        given(weatherValueOps.get(CACHE_KEY)).willReturn(cached);

        given(weatherService.resolveMidTermRegId(SIDO_NAME)).willReturn("11B10101");
        given(midTermTemperatureRedisTemplate.opsForValue()).willReturn(midTermValueOps);
        given(midTermValueOps.get("weather:midterm:11B10101")).willReturn(null);
        given(weatherService.midTermTemperature(SIDO_NAME, BASE_DATE, BASE_TIME))
                .willThrow(new RuntimeException("중기기온 API 장애"));

        WeatherDataDto result = weatherCacheService.getWeatherDate(GRID_X, GRID_Y, SIDO_NAME, CITY_NAME, BASE_DATE,
                BASE_TIME);

        assertThat(result.midTermTemperature()).isNull();
        verify(midTermValueOps, never()).set(anyString(), any(MidTermTemperatureDto.class), any(Duration.class));
    }

    // ---------------------------------------------------------------
    // refreshCurrentWeather
    // ---------------------------------------------------------------

    @Test
    void refreshCurrentWeather_기존_캐시가_있으면_현재날씨와_초단기예보만_갱신한다() {
        given(stringRedisTemplate.opsForValue()).willReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class))).willReturn(true);

        WeatherDataDto existing = sampleWeatherData(null);
        given(weatherRedisTemplate.opsForValue()).willReturn(weatherValueOps);
        given(weatherValueOps.get(CACHE_KEY)).willReturn(existing);

        CurrentWeatherDto newCurrent = new CurrentWeatherDto("25", "40", "0", "없음");
        UltraForecastWeatherDto newUltra = new UltraForecastWeatherDto("2");
        given(weatherService.currentWeather(GRID_X, GRID_Y, BASE_DATE, BASE_TIME)).willReturn(newCurrent);
        given(weatherService.ultraForecastWeather(GRID_X, GRID_Y, BASE_DATE, BASE_TIME)).willReturn(newUltra);

        given(weatherRedisTemplate.getExpire(CACHE_KEY, TimeUnit.SECONDS)).willReturn(300L);

        weatherCacheService.refreshCurrentWeather(GRID_X, GRID_Y, BASE_DATE, BASE_TIME);

        WeatherDataDto expectedUpdated = new WeatherDataDto(newCurrent, existing.forecast(), newUltra,
                existing.airQuality(), null);
        verify(weatherValueOps).set(CACHE_KEY, expectedUpdated, Duration.ofSeconds(300));
        verify(stringRedisTemplate).execute(any(RedisScript.class), eq(List.of(LOCK_KEY)), any());
    }

    @Test
    void refreshCurrentWeather_락_획득에_실패하면_아무것도_하지않는다() {
        given(stringRedisTemplate.opsForValue()).willReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class))).willReturn(false);

        weatherCacheService.refreshCurrentWeather(GRID_X, GRID_Y, BASE_DATE, BASE_TIME);

        verifyNoInteractions(weatherRedisTemplate, weatherService);
        verify(stringRedisTemplate, never()).execute(any(RedisScript.class), any(List.class), any());
    }

    @Test
    void refreshCurrentWeather_기존_캐시가_없으면_갱신을_건너뛰지만_락은_해제한다() {
        given(stringRedisTemplate.opsForValue()).willReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class))).willReturn(true);

        given(weatherRedisTemplate.opsForValue()).willReturn(weatherValueOps);
        given(weatherValueOps.get(CACHE_KEY)).willReturn(null);

        weatherCacheService.refreshCurrentWeather(GRID_X, GRID_Y, BASE_DATE, BASE_TIME);

        verifyNoInteractions(weatherService);
        verify(weatherValueOps, never()).set(anyString(), any(WeatherDataDto.class), any(Duration.class));
        verify(stringRedisTemplate).execute(any(RedisScript.class), eq(List.of(LOCK_KEY)), any());
    }

    // ---------------------------------------------------------------
    // refreshVillageForecast
    // ---------------------------------------------------------------

    @Test
    void refreshVillageForecast_기존_캐시가_있으면_단기예보만_갱신한다() {
        given(stringRedisTemplate.opsForValue()).willReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class))).willReturn(true);

        WeatherDataDto existing = sampleWeatherData(null);
        given(weatherRedisTemplate.opsForValue()).willReturn(weatherValueOps);
        given(weatherValueOps.get(CACHE_KEY)).willReturn(existing);

        ForecastWeatherDto newForecast = new ForecastWeatherDto("26", "29", "20", "흐림", "60", "비");
        given(weatherService.forecastWeather(GRID_X, GRID_Y, BASE_DATE, BASE_TIME)).willReturn(newForecast);

        given(weatherRedisTemplate.getExpire(CACHE_KEY, TimeUnit.SECONDS)).willReturn(300L);

        weatherCacheService.refreshVillageForecast(GRID_X, GRID_Y, BASE_DATE, BASE_TIME);

        WeatherDataDto expectedUpdated = new WeatherDataDto(existing.current(), newForecast,
                existing.ultraForecastWeather(), existing.airQuality(), null);
        verify(weatherValueOps).set(CACHE_KEY, expectedUpdated, Duration.ofSeconds(300));
    }

    // ---------------------------------------------------------------
    // refreshAirQuality
    // ---------------------------------------------------------------

    @Test
    void refreshAirQuality_기존_캐시가_있으면_미세먼지만_갱신한다() {
        given(stringRedisTemplate.opsForValue()).willReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class))).willReturn(true);

        WeatherDataDto existing = sampleWeatherData(null);
        given(weatherRedisTemplate.opsForValue()).willReturn(weatherValueOps);
        given(weatherValueOps.get(CACHE_KEY)).willReturn(existing);

        AirQualityDto newAirQuality = new AirQualityDto("80", "45", "78", "44", "나쁨", "나쁨");
        given(weatherService.airQuality(GRID_X, GRID_Y, SIDO_NAME, CITY_NAME, BASE_DATE, BASE_TIME))
                .willReturn(newAirQuality);

        given(weatherRedisTemplate.getExpire(CACHE_KEY, TimeUnit.SECONDS)).willReturn(300L);

        weatherCacheService.refreshAirQuality(GRID_X, GRID_Y, SIDO_NAME, CITY_NAME, BASE_DATE, BASE_TIME);

        WeatherDataDto expectedUpdated = new WeatherDataDto(existing.current(), existing.forecast(),
                existing.ultraForecastWeather(), newAirQuality, null);
        verify(weatherValueOps).set(CACHE_KEY, expectedUpdated, Duration.ofSeconds(300));
    }

    @Test
    void refreshAirQuality_기존_캐시가_없으면_전체_데이터를_새로_적재한다() {
        given(stringRedisTemplate.opsForValue()).willReturn(stringValueOps);
        given(stringValueOps.setIfAbsent(eq(LOCK_KEY), anyString(), any(Duration.class))).willReturn(true);

        given(weatherRedisTemplate.opsForValue()).willReturn(weatherValueOps);
        given(weatherValueOps.get(CACHE_KEY)).willReturn(null);

        WeatherDataDto fresh = sampleWeatherData(null);
        given(weatherService.fetchWeatherData(GRID_X, GRID_Y, SIDO_NAME, CITY_NAME, BASE_DATE, BASE_TIME))
                .willReturn(fresh);

        weatherCacheService.refreshAirQuality(GRID_X, GRID_Y, SIDO_NAME, CITY_NAME, BASE_DATE, BASE_TIME);

        verify(weatherValueOps).set(eq(CACHE_KEY), eq(fresh), any(Duration.class));
    }
}
