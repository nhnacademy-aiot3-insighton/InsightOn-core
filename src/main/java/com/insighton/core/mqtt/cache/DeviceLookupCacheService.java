package com.insighton.core.mqtt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.insighton.core.mqtt.cache.dto.DeviceCacheEntry;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * devEui 기준으로 기기 식별 정보를 Caffeine → Redis → PostgreSQL 순으로 조회하는 3계층 캐싱 서비스.
 * 상위 계층이 미스면 하위 계층에서 찾은 값을 상위 계층에 채워 넣는 populate-on-miss 구조로 동작함.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceLookupCacheService {
    private static final String REDIS_KEY_PREFIX = "device:eui:";
    private static final Duration REDIS_TTL = Duration.ofHours(6);

    private final Cache<String, DeviceCacheEntry> deviceEuiLocalCache;
    private final RedisTemplate<String, DeviceCacheEntry> deviceRedisTemplate;
    //TODO: DeviceRepository 주입

    /**
     * 기기 EUI로 캐시된 기기 식별 정보를 조회하고, Redis 조회 결과를 로컬 캐시에 저장합니다.
     *
     * @param deviceEui 조회할 기기의 고유 식별자
     * @return 조회된 캐시 항목이 있으면 해당 값, 조회되지 않으면 빈 {@code Optional}
     */
    public Optional<DeviceCacheEntry> lookup(String deviceEui) {
        DeviceCacheEntry local = deviceEuiLocalCache.getIfPresent(deviceEui);

        if (local != null) {
            return Optional.of(local);
        }

        DeviceCacheEntry fromRedis = deviceRedisTemplate.opsForValue().get(REDIS_KEY_PREFIX + deviceEui);
        if (fromRedis != null) {
            deviceEuiLocalCache.put(deviceEui, fromRedis);
            return Optional.of(fromRedis);
        }

        Optional<DeviceCacheEntry> fromDb = Optional.empty(); //TODO: DeviceRepository 호출해 DeviceCacheEntry 반환해주는 메서드 필요

        fromDb.ifPresent(this::populate);

        return fromDb;
    }

    /**
     * 기기 정보를 로컬 캐시와 Redis에 저장합니다.
     *
     * @param entry 저장할 기기 정보
     */
    public void populate(DeviceCacheEntry entry) {
        deviceEuiLocalCache.put(entry.deviceEui(), entry);
        deviceRedisTemplate.opsForValue().set(REDIS_KEY_PREFIX + entry.deviceEui(), entry, REDIS_TTL);
    }

    /**
     * 기기의 캐시 항목을 로컬 캐시와 Redis에서 삭제합니다.
     *
     * @param deviceEui 삭제할 기기의 고유 식별자
     */
    public void evict(String deviceEui) {
        deviceEuiLocalCache.invalidate(deviceEui);
        deviceRedisTemplate.delete(REDIS_KEY_PREFIX + deviceEui);
    }
}