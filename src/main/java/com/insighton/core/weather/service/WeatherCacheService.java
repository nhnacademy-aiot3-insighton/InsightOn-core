package com.insighton.core.weather.service;

import com.insighton.core.weather.dto.WeatherDataDto;
import com.insighton.core.weather.util.CacheTimeUtils;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final WeatherIntegrationService weatherIntegrationService;

    public WeatherDataDto getWeatherDate(int gridX, int gridY, String sidoName, String cityName, String baseDate,
                                         String baseTime) {
        String cacheKey = String.format("weather:%d:%d", gridX, gridY);

        WeatherDataDto cachedData = (WeatherDataDto) redisTemplate.opsForValue().get(cacheKey);

        if (cachedData != null) {
            log.info("[Cache hit] key: {}", cacheKey);
            return cachedData;
        }

        log.info("[Cache Miss] key: {} - 외부 API를 호출하여 데이터를 갱신합니다.", cacheKey);

        WeatherDataDto freshData = weatherIntegrationService.fetchWeatherData(gridX, gridY, sidoName, cityName,
                baseDate, baseTime);

        if (freshData != null) {
            Duration ttl = CacheTimeUtils.getDurationUtilNextHour();
            log.info("Key: {} 의 동적 TTL 설정: {}분 {}초", cacheKey, ttl.toMinutes(), ttl.toSecondsPart());

            redisTemplate.opsForValue().set(cacheKey, freshData, ttl);
        }

        return freshData;
    }
}
