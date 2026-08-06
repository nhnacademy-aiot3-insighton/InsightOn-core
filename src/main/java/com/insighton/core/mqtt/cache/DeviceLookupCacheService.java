package com.insighton.core.mqtt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.insighton.core.mqtt.cache.dto.DeviceCacheEntry;
import com.insighton.core.sensors.entity.Device;
import com.insighton.core.sensors.repository.DeviceRepository;
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
    private final DeviceRepository deviceRepository;

    /**
     * devEui로 기기 캐시 항목을 조회함. Caffeine, Redis, PostgreSQL 순으로 확인하며
     * 도중에 값을 찾으면 상위 계층에 채워 넣은 뒤 반환함. 어디에도 없으면 빈 Optional을 반환함
     * (Auto-Provisioning 대상 — 이후 {@link #populate(DeviceCacheEntry)} 호출로 캐시가 채워짐).
     *
     * @param deviceEui 조회할 기기 고유 식별자
     * @return 조회된 캐시 항목, 완전 미스 시 빈 Optional
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

        Optional<DeviceCacheEntry> fromDb = deviceRepository.findByDeviceEui(deviceEui)
                .map(this::toCacheEntry);

        fromDb.ifPresent(this::populate);

        return fromDb;
    }

    /**
     * 캐시 항목을 Caffeine과 Redis 양쪽에 채워 넣음.
     * DB 조회로 새로 찾았거나 Auto-Provisioning으로 새 기기가 생성됐을 때 호출함.
     *
     * @param entry 캐시에 채워 넣을 기기 정보
     */
    public void populate(DeviceCacheEntry entry) {
        deviceEuiLocalCache.put(entry.deviceEui(), entry);
        deviceRedisTemplate.opsForValue().set(REDIS_KEY_PREFIX + entry.deviceEui(), entry, REDIS_TTL);
    }

    /**
     * 캐시 항목을 Caffeine과 Redis 양쪽에서 제거함. 기기 정보가 변경/삭제됐을 때 사용함.
     *
     * @param deviceEui 제거할 기기의 고유 식별자
     */
    public void evict(String deviceEui) {
        deviceEuiLocalCache.invalidate(deviceEui);
        deviceRedisTemplate.delete(REDIS_KEY_PREFIX + deviceEui);
    }

    private DeviceCacheEntry toCacheEntry(Device device) {
        return new DeviceCacheEntry(
                device.getDeviceId(),
                device.getDeviceEui(),
                device.getGateway().getGatewayId(),
                device.getLocation().getLocationId()
        );
    }
}