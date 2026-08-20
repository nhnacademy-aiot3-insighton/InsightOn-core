package com.insighton.core.domain.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.insighton.core.domain.weather.dto.CurrentWeatherDto;
import com.insighton.core.domain.weather.dto.UltraForecastWeatherDto;
import com.insighton.core.domain.weather.dto.WeatherDataDto;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
class WeatherCacheServiceTest {

    private WeatherCacheService weatherCacheService;

    @Mock
    private RedisTemplate<String, WeatherDataDto> weatherRedisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private WeatherService weatherService;

    @Mock
    private ValueOperations<String, WeatherDataDto> weatherValueOperations;

    @Mock
    private ValueOperations<String, String> stringValueOperations;

    @BeforeEach
    void setUp() {
        weatherCacheService = new WeatherCacheService(weatherRedisTemplate, stringRedisTemplate, weatherService);
    }

    @Test
    @DisplayName("캐시 히트 성공 - Redis에 데이터가 존재할 경우 외부 API를 호출하지 않음")
    void getWeatherDate_CacheHit() {
        // given
        int gridX = 60, gridY = 127;
        String cacheKey = "weather:grid:60:127";
        WeatherDataDto mockWeatherData = new WeatherDataDto(null, null, null, null);

        given(weatherRedisTemplate.opsForValue()).willReturn(weatherValueOperations);
        given(weatherValueOperations.get(cacheKey)).willReturn(mockWeatherData);

        // when
        WeatherDataDto result = weatherCacheService.getWeatherDate(gridX, gridY, "서울특별시", "강남구", "20260601", "1200");

        // then
        assertThat(result).isNotNull();
        verify(weatherService, never()).fetchWeatherData(anyInt(), anyInt(), anyString(), anyString(), anyString(),
                anyString());
    }

    @Test
    @DisplayName("캐시 미스 및 락 획득 성공 - 외부 API 호출 후 Redis에 적재")
    void getWeatherDate_CacheMiss_AcquireLock() {
        // given
        int gridX = 60, gridY = 127;
        String cacheKey = "weather:grid:60:127";
        String lockKey = cacheKey + ":lock";
        WeatherDataDto mockWeatherData = new WeatherDataDto(null, null, null, null);

        given(weatherRedisTemplate.opsForValue()).willReturn(weatherValueOperations);
        given(stringRedisTemplate.opsForValue()).willReturn(stringValueOperations);

        // 1차 캐시 미스
        given(weatherValueOperations.get(cacheKey)).willReturn(null);
        // 락 획득 성공
        given(stringValueOperations.setIfAbsent(eq(lockKey), anyString(), any(Duration.class))).willReturn(true);
        // Double-check 캐시 미스
        given(weatherValueOperations.get(cacheKey)).willReturn(null);
        // 외부 API 호출 결과
        given(weatherService.fetchWeatherData(eq(gridX), eq(gridY), eq("서울특별시"), eq("강남구"), eq("20260601"), eq("1200")))
                .willReturn(mockWeatherData);

        // when
        WeatherDataDto result = weatherCacheService.getWeatherDate(gridX, gridY, "서울특별시", "강남구", "20260601", "1200");

        // then
        assertThat(result).isNotNull();
        verify(weatherService).fetchWeatherData(eq(gridX), eq(gridY), eq("서울특별시"), eq("강남구"), eq("20260601"),
                eq("1200"));
        verify(weatherValueOperations).set(eq(cacheKey), eq(mockWeatherData), any(Duration.class));
        verify(stringRedisTemplate).execute(any(DefaultRedisScript.class),
                eq(java.util.Collections.singletonList(lockKey)), anyString());
    }

    @Test
    @DisplayName("초단기 날씨 부분 갱신 성공 (스케줄러)")
    void refreshCurrentWeather_Success() {
        // given
        int gridX = 60, gridY = 127;
        String cacheKey = "weather:grid:60:127";
        String lockKey = cacheKey + ":lock";

        CurrentWeatherDto newCurrent = new CurrentWeatherDto("25.0", "50", "0.0", null);
        UltraForecastWeatherDto newUltra = new UltraForecastWeatherDto("0.0");
        WeatherDataDto cachedData = new WeatherDataDto(null, null, null, null);

        given(stringRedisTemplate.opsForValue()).willReturn(stringValueOperations);
        given(weatherRedisTemplate.opsForValue()).willReturn(weatherValueOperations);

        // 락 획득 성공
        given(stringValueOperations.setIfAbsent(eq(lockKey), anyString(), any(Duration.class))).willReturn(true);
        // 기존 캐시 존재
        given(weatherValueOperations.get(cacheKey)).willReturn(cachedData);
        given(weatherService.currentWeather(gridX, gridY, "20260601", "1200")).willReturn(newCurrent);
        given(weatherService.ultraForecastWeather(gridX, gridY, "20260601", "1200")).willReturn(newUltra);
        given(weatherRedisTemplate.getExpire(eq(cacheKey), any())).willReturn(1800L);

        // when
        weatherCacheService.refreshCurrentWeather(gridX, gridY, "20260601", "1200");

        // then
        verify(weatherValueOperations).set(eq(cacheKey), any(WeatherDataDto.class), any(Duration.class));
        verify(stringRedisTemplate).execute(any(DefaultRedisScript.class),
                eq(java.util.Collections.singletonList(lockKey)), anyString());
    }
}