package com.insighton.core.weather.service;

import com.insighton.core.weather.dto.WeatherDataDto;
import com.insighton.core.weather.exception.WeatherApiException;
import com.insighton.core.weather.util.CacheTimeUtils;
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

        // 2. 고유 요청 토큰 생성, 락 획득 시도
        String lockKey = cacheKey + ":lock";
        String lockToken = UUID.randomUUID().toString();
        boolean acquired = acquireLock(lockKey, lockToken);

        if (!acquired) {
            // 락 획득 실패시 10초 전체 TTL 범위 내에서 재시도 및 대기
            return waitForCache(cacheKey, lockKey, lockToken);
        }

        try {
            // 3. Double-Check
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
            // 5. 본인의 락 토큰 검증 후 안전하게 락 해제
            releaseLock(lockKey, lockToken);
        }
    }

    // UUID 토큰 기반 Lock 획득
    private boolean acquireLock(String lockKey, String lockToken) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, LOCK_TTL);
        return Boolean.TRUE.equals(success);
    }

    // Lua 스크립트를 통한 원자적 Compare-and-Delete 락 해제
    private void releaseLock(String lockKey, String lockToken) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class);
        stringRedisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockToken);
    }

    private WeatherDataDto waitForCache(String cacheKey, String lockKey, String lockToken) {
        long deadline = System.currentTimeMillis() + LOCK_TTL.toMillis();

        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WeatherApiException("락 대기 중 인터럽트 발생");
            }

            WeatherDataDto cachedData = weatherRedisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.info("[Cache hit after wait] key: {}", cacheKey);
                return cachedData;
            }

            if (acquireLock(lockKey, lockToken)) {
                try {
                    cachedData = weatherRedisTemplate.opsForValue().get(cacheKey);
                    if (cachedData != null) {
                        return cachedData;
                    }
                } finally {
                    releaseLock(lockKey, lockToken);
                }
            }
        }

        throw new WeatherApiException("날씨 데이터 캐시 적재 대기 시간 초과 (Lock Timeout)");
    }
}