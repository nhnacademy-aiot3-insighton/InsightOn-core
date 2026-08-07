package com.insighton.core.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.insighton.core.weather.dto.WeatherDataDto;
import com.insighton.core.weather.util.CacheTimeUtils;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherCacheServiceTest {

    @Mock
    private RedisTemplate<String, WeatherDataDto> weatherRedisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private WeatherIntegrationService weatherIntegrationService;

    @Mock
    private ValueOperations<String, WeatherDataDto> weatherValueOps;

    @Mock
    private ValueOperations<String, String> stringValueOps;

    // @InjectMocks 대신 직접 생성자로 주입하여 Mock 매핑 오류 원천 차단
    private WeatherCacheService weatherCacheService;

    private WeatherDataDto dummyData;
    private final String cacheKey = "weather:grid:58:74";
    private final String lockKey = cacheKey + ":lock";

    @BeforeEach
    void setUp() {
        // opsForValue() 스텁 설정
        lenient().when(weatherRedisTemplate.opsForValue()).thenReturn(weatherValueOps);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOps);

        // 생성자를 통한 명확한 Mock 객체 주입 (파라미터 꼬임 해결)
        weatherCacheService = new WeatherCacheService(
                weatherRedisTemplate,
                stringRedisTemplate,
                weatherIntegrationService
        );

        dummyData = new WeatherDataDto(
                new WeatherDataDto.CurrentWeather("20.0", "50", "0", "없음"),
                new WeatherDataDto.ForecastWeather("25.0", "15.0", "맑음", "0", "없음"),
                new WeatherDataDto.AirQuality("15", "8", "15", "8", "좋음", "좋음")
        );
    }

    @Test
    @DisplayName("[1차 캐시 Hit] Redis에 데이터가 존재하면 외부 API를 호출하지 않고 캐시 반환")
    void getWeatherDate_CacheHit() {
        // given
        lenient().when(weatherValueOps.get(eq(cacheKey))).thenReturn(dummyData);

        // when
        WeatherDataDto result = weatherCacheService.getWeatherDate(58, 74, "광주", "동구", "20260806", "1200");

        // then
        assertThat(result).isNotNull();
        assertThat(result.current().temp()).isEqualTo("20.0");
        verify(weatherIntegrationService, never()).fetchWeatherData(anyInt(), anyInt(), anyString(), anyString(),
                anyString(), anyString());
    }

    @Test
    @DisplayName("[Cache Miss + Lock 획득 성공] 캐시 미스 시 락을 획득하고 외부 API 호출 후 정각 기반 TTL 검증")
    void getWeatherDate_CacheMiss_LockAcquired_LoadAndCache() {
        // given
        lenient().when(weatherValueOps.get(eq(cacheKey))).thenReturn(null);
        lenient().when(stringValueOps.setIfAbsent(eq(lockKey), anyString(), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        lenient().when(
                        weatherIntegrationService.fetchWeatherData(anyInt(), anyInt(), anyString(), anyString(), anyString(),
                                anyString()))
                .thenReturn(dummyData);

        // when
        WeatherDataDto result = weatherCacheService.getWeatherDate(58, 74, "광주", "동구", "20260806", "1200");

        // then
        assertThat(result).isNotNull();
        assertThat(result.forecast().skyStatus()).isEqualTo("맑음");

        // TTL 검증
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(weatherValueOps, times(1)).set(eq(cacheKey), eq(dummyData), ttlCaptor.capture());

        Duration actualTtl = ttlCaptor.getValue();
        Duration expectedTtl = CacheTimeUtils.getDurationUtilNextHour();

        assertThat(actualTtl).isNotNull();
        assertThat(actualTtl.getSeconds()).isPositive();
        assertThat(actualTtl.getSeconds()).isLessThanOrEqualTo(3600);
        assertThat(Math.abs(actualTtl.getSeconds() - expectedTtl.getSeconds())).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("[Cache Miss + Lock 대기 후 Hit] 타 스레드가 락 선점 시 대기하다가 적재된 캐시 반환")
    void getWeatherDate_CacheMiss_LockFailed_WaitForCache_Success() {
        // given
        lenient().when(stringValueOps.setIfAbsent(eq(lockKey), anyString(), any(Duration.class)))
                .thenReturn(Boolean.FALSE);

        // 1번째 호출은 null, 2번째(waitForCache 대기 루프 내) 호출은 dummyData 리턴
        lenient().when(weatherValueOps.get(eq(cacheKey)))
                .thenReturn(null)
                .thenReturn(dummyData);

        // when
        WeatherDataDto result = weatherCacheService.getWeatherDate(58, 74, "광주", "동구", "20260806", "1200");

        // then
        assertThat(result).isNotNull();
        assertThat(result.current().temp()).isEqualTo("20.0");
        verify(weatherIntegrationService, never()).fetchWeatherData(anyInt(), anyInt(), anyString(), anyString(),
                anyString(), anyString());
    }

    @Test
    @DisplayName("[Cache Miss + Lock 대기 후 재선점] 이전 선점자 만료 시 대기 스레드가 락 재선점 후 동일한 TTL 단언 적재")
    void getWeatherDate_CacheMiss_LockFailed_ReAcquireLock_AndCache() {
        // given
        lenient().when(weatherValueOps.get(eq(cacheKey))).thenReturn(null);

        // 첫번째 락 실패 -> 루프 내 두번째 락 성공
        lenient().when(stringValueOps.setIfAbsent(eq(lockKey), anyString(), any(Duration.class)))
                .thenReturn(Boolean.FALSE, Boolean.TRUE);

        lenient().when(
                        weatherIntegrationService.fetchWeatherData(anyInt(), anyInt(), anyString(), anyString(), anyString(),
                                anyString()))
                .thenReturn(dummyData);

        // when
        WeatherDataDto result = weatherCacheService.getWeatherDate(58, 74, "광주", "동구", "20260806", "1200");

        // then
        assertThat(result).isNotNull();
        verify(weatherIntegrationService, times(1)).fetchWeatherData(58, 74, "광주", "동구", "20260806", "1200");

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(weatherValueOps, times(1)).set(eq(cacheKey), eq(dummyData), ttlCaptor.capture());

        Duration actualTtl = ttlCaptor.getValue();
        Duration expectedTtl = CacheTimeUtils.getDurationUtilNextHour();

        assertThat(actualTtl).isNotNull();
        assertThat(actualTtl.getSeconds()).isPositive();
        assertThat(actualTtl.getSeconds()).isLessThanOrEqualTo(3600);
        assertThat(Math.abs(actualTtl.getSeconds() - expectedTtl.getSeconds())).isLessThanOrEqualTo(2);
    }
}