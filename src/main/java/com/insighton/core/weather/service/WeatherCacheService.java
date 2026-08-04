package com.insighton.core.weather.service;

import com.insighton.core.weather.dto.WeatherDataDto;
import com.insighton.core.weather.util.CacheTimeUtils;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherCacheService {

    private final RedisTemplate<String, WeatherDataDto> weatherRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final WeatherIntegrationService weatherIntegrationService;

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

        // 2. 분산 락 획득 시도 (SET NX EX)
        String lockKey = cacheKey + ":lock";
        boolean acquired = acquireLock(lockKey);

        if (!acquired) {
            // 락 획득 실패 시 다른 스레드가 캐시를 채울 때까지 대기 후 재조회
            return waitForCache(cacheKey);
        }

        try {
            // 3. Double-Check: 락을 얻은 직후 캐시 재확인
            cachedData = weatherRedisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.info("[Cache hit after lock] key: {}", cacheKey);
                return cachedData;
            }

            // 4. 외부 API 호출 및 캐시 저장
            log.info("[Cache Miss Validated] key: {} - 외부 API 호출 실행", cacheKey);
            WeatherDataDto freshData = weatherIntegrationService.fetchWeatherData(gridX, gridY, sidoName, cityName,
                    baseDate, baseTime);

            if (freshData != null) {
                Duration ttl = CacheTimeUtils.getDurationUtilNextHour();
                log.info("Key: {} 의 동적 TTL 설정: {}분 {}초", cacheKey, ttl.toMinutes(), ttl.toSecondsPart());
                weatherRedisTemplate.opsForValue().set(cacheKey, freshData, ttl);
            }

            return freshData;
        } finally {
            // 5. 작업 후 안전하게 락 해제
            releaseLock(lockKey);
        }
    }

    private boolean acquireLock(String lockKey) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(10));
        return Boolean.TRUE.equals(success);
    }

    private void releaseLock(String lockKey) {
        weatherRedisTemplate.delete(lockKey);
    }

    private WeatherDataDto waitForCache(String cacheKey) {
        int maxRetries = 20;
        int retries = 0;

        while (retries < maxRetries) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            WeatherDataDto cachedData = weatherRedisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.info("[Cache hit after wait] key: {}", cacheKey);
                return cachedData;
            }
            retries++;
        }

        log.warn("[Lock Wait Timeout] key: {} - 대기 시간 초과로 직접 반환 시도", cacheKey);
        return null;
    }
}