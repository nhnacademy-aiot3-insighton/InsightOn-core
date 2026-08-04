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

        // 2. 고유 요청 토큰 생성 및 락 획득 시도
        String lockKey = cacheKey + ":lock";
        String lockToken = UUID.randomUUID().toString();
        boolean acquired = acquireLock(lockKey, lockToken);

        if (!acquired) {
            // 락 획득 실패 시 대기 루프 (재선점 시 직접 캐시 채움 수행을 위해 파라미터 전달)
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
        WeatherDataDto freshData = weatherIntegrationService.fetchWeatherData(gridX, gridY, sidoName, cityName,
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