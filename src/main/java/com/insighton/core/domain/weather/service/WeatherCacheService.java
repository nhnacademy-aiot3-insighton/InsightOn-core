package com.insighton.core.domain.weather.service;

import com.insighton.core.domain.weather.dto.AirQualityDto;
import com.insighton.core.domain.weather.dto.CurrentWeatherDto;
import com.insighton.core.domain.weather.dto.ForecastWeatherDto;
import com.insighton.core.domain.weather.dto.UltraForecastWeatherDto;
import com.insighton.core.domain.weather.dto.WeatherDataDto;
import com.insighton.core.domain.weather.exception.WeatherApiException;
import com.insighton.core.domain.weather.util.CacheTimeUtils;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherCacheService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end";

    private final RedisTemplate<String, WeatherDataDto> weatherRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final WeatherService weatherService;

    public WeatherDataDto getWeatherDate(int gridX, int gridY, String sidoName, String cityName, String baseDate,
                                         String baseTime) {
        String cacheKey = String.format("weather:grid:%d:%d", gridX, gridY);

        // 1. 1차 캐시 확인
        WeatherDataDto cachedData = weatherRedisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            log.info("[Cache hit] key: {}", cacheKey);
            return cachedData;
        }

        log.info("[Cache Miss] key: {} - 동시성 제어를 위한 락 시도", cacheKey);

        // 2. 고유 요청 토큰 생성 및 락 획득 시도
        String lockKey = cacheKey + ":lock";
        String lockToken = UUID.randomUUID().toString();
        boolean acquired = acquireLock(lockKey, lockToken);

        if (!acquired) {
            return waitForCache(cacheKey, lockKey, lockToken, gridX, gridY, sidoName, cityName, baseDate, baseTime);
        }

        try {
            // 3. 락 획득 성공 시 공통 로딩/적재 메서드 수행
            return loadAndCacheData(cacheKey, gridX, gridY, sidoName, cityName, baseDate, baseTime);
        } finally {
            // 4. 본인 토큰 검증 후 락 해제
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * 초단기실황 및 초단기예보만 부분 갱신 (스케줄러용)
     */
    public void refreshCurrentWeather(int gridX, int gridY, String baseDate, String baseTime) {
        String cacheKey = String.format("weather:grid:%d:%d", gridX, gridY);
        WeatherDataDto cachedData = weatherRedisTemplate.opsForValue().get(cacheKey);

        // 캐시에 데이터가 없는 경우를 대비한 방어 로직 (필요시 sidoName/cityName을 빈 값 처리하거나 기본 조회 수행)
        if (cachedData == null) {
            log.info("[초단기 갱신] 기존 캐시가 없어 전체 데이터를 새로 적재합니다. key: {}", cacheKey);
            getWeatherDate(gridX, gridY, "", "", baseDate, baseTime);
            return;
        }

        CurrentWeatherDto newCurrent = weatherService.currentWeather(gridX, gridY, baseDate, baseTime);
        UltraForecastWeatherDto newUltraForecast = weatherService.ultraForecastWeather(gridX, gridY, baseDate,
                baseTime);

        WeatherDataDto updatedData = new WeatherDataDto(
                newCurrent,
                cachedData.forecast(),
                newUltraForecast,
                cachedData.airQuality()
        );

        Duration ttl = CacheTimeUtils.getDurationUtilNextHour();
        weatherRedisTemplate.opsForValue().set(cacheKey, updatedData, ttl);
        log.info("[초단기 부분 갱신 완료] key: {}", cacheKey);
    }

    /**
     * 단기예보만 부분 갱신 (스케줄러용)
     */
    public void refreshVillageForecast(int gridX, int gridY, String baseDate, String baseTime) {
        String cacheKey = String.format("weather:grid:%d:%d", gridX, gridY);
        WeatherDataDto cachedData = weatherRedisTemplate.opsForValue().get(cacheKey);

        if (cachedData == null) {
            log.info("[단기예보 갱신] 기존 캐시가 없어 전체 데이터를 새로 적재합니다. key: {}", cacheKey);
            getWeatherDate(gridX, gridY, "", "", baseDate, baseTime);
            return;
        }

        ForecastWeatherDto newForecast = weatherService.forecastWeather(gridX, gridY, baseDate, baseTime);

        WeatherDataDto updatedData = new WeatherDataDto(
                cachedData.current(),
                newForecast,
                cachedData.ultraForecastWeather(),
                cachedData.airQuality()
        );

        Duration ttl = CacheTimeUtils.getDurationUtilNextHour();
        weatherRedisTemplate.opsForValue().set(cacheKey, updatedData, ttl);
        log.info("[단기예보 부분 갱신 완료] key: {}", cacheKey);
    }

    /**
     * 미세먼지만 부분 갱신 (스케줄러용)
     */
    public void refreshAirQuality(int gridX, int gridY, String sidoName, String cityName, String baseDate,
                                  String baseTime) {
        String cacheKey = String.format("weather:grid:%d:%d", gridX, gridY);
        WeatherDataDto cachedData = weatherRedisTemplate.opsForValue().get(cacheKey);

        if (cachedData == null) {
            log.info("[미세먼지 갱신] 기존 캐시가 없어 전체 데이터를 새로 적재합니다. key: {}", cacheKey);
            getWeatherDate(gridX, gridY, sidoName, cityName, baseDate, baseTime);
            return;
        }

        AirQualityDto newAirQuality = weatherService.airQuality(gridX, gridY, sidoName, cityName, baseDate, baseTime);

        WeatherDataDto updatedData = new WeatherDataDto(
                cachedData.current(),
                cachedData.forecast(),
                cachedData.ultraForecastWeather(),
                newAirQuality
        );

        Duration ttl = CacheTimeUtils.getDurationUtilNextHour();
        weatherRedisTemplate.opsForValue().set(cacheKey, updatedData, ttl);
        log.info("[미세먼지 부분 갱신 완료] key: {}", cacheKey);
    }

    /**
     * 캐시 재확인 -> 미스 시 외부 API 호출 및 Redis 적재를 수행하는 공통 메서드
     */
    private WeatherDataDto loadAndCacheData(String cacheKey, int gridX, int gridY, String sidoName, String cityName,
                                            String baseDate, String baseTime) {
        // Double-Check
        WeatherDataDto cachedData = weatherRedisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            log.info("[Cache hit after lock] key: {}", cacheKey);
            return cachedData;
        }

        log.info("[Cache Miss Validated] key: {} - 외부 API 호출 실행", cacheKey);
        WeatherDataDto freshData = weatherService.fetchWeatherData(gridX, gridY, sidoName, cityName,
                baseDate, baseTime);

        if (freshData != null) {
            Duration ttl = CacheTimeUtils.getDurationUtilNextHour();
            log.info("Key: {} 의 동적 TTL 설정: {}분 {}초", cacheKey, ttl.toMinutes(), ttl.toSecondsPart());
            weatherRedisTemplate.opsForValue().set(cacheKey, freshData, ttl);
        }

        return freshData;
    }

    private boolean acquireLock(String lockKey, String lockToken) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, LOCK_TTL);
        return Boolean.TRUE.equals(success);
    }

    private void releaseLock(String lockKey, String lockToken) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class);
        stringRedisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockToken);
    }

    private WeatherDataDto waitForCache(String cacheKey, String lockKey, String lockToken,
                                        int gridX, int gridY, String sidoName, String cityName, String baseDate,
                                        String baseTime) {
        long deadline = System.currentTimeMillis() + LOCK_TTL.toMillis();

        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WeatherApiException("락 대기 중 인터럽트 발생");
            }

            // 1) 이전 선점자가 채워둔 캐시 확인
            WeatherDataDto cachedData = weatherRedisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.info("[Cache hit after wait] key: {}", cacheKey);
                return cachedData;
            }

            // 2) 락을 재선점한 경우: 단순 조회에 그치지 않고 full cache-fill 흐름(loadAndCacheData) 실행
            if (acquireLock(lockKey, lockToken)) {
                try {
                    log.info("[Re-acquired Lock] key: {} - 대기 스레드가 락을 획득하여 캐시 적재를 진행합니다.", cacheKey);
                    return loadAndCacheData(cacheKey, gridX, gridY, sidoName, cityName, baseDate, baseTime);
                } finally {
                    releaseLock(lockKey, lockToken);
                }
            }
        }

        throw new WeatherApiException("날씨 데이터 캐시 적재 대기 시간 초과 (Lock Timeout)");
    }
}