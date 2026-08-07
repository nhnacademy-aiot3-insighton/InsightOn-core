package com.insighton.core.mqtt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.insighton.core.mqtt.cache.dto.SensorCacheEntry;
import com.insighton.core.sensors.entity.Sensor;
import com.insighton.core.sensors.repository.SensorRepository;
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
public class SensorLookupCacheService {
    private static final String REDIS_KEY_PREFIX = "sensor:eui:";
    private static final Duration REDIS_TTL = Duration.ofHours(6);

    private final Cache<String, SensorCacheEntry> sensorEuiLocalCache;
    private final RedisTemplate<String, SensorCacheEntry> sensorRedisTemplate;
    private final SensorRepository sensorRepository;

    /**
     * devEui로 기기 캐시 항목을 조회함. Caffeine, Redis, PostgreSQL 순으로 확인하며
     * 도중에 값을 찾으면 상위 계층에 채워 넣은 뒤 반환함. 어디에도 없으면 빈 Optional을 반환함
     * (Auto-Provisioning 대상 — 이후 {@link #populate(SensorCacheEntry)} 호출로 캐시가 채워짐).
     *
     * @param sensorEui 조회할 기기 고유 식별자
     * @return 조회된 캐시 항목, 완전 미스 시 빈 Optional
     */
    public Optional<SensorCacheEntry> lookup(String sensorEui) {
        SensorCacheEntry local = sensorEuiLocalCache.getIfPresent(sensorEui);

        if (local != null) {
            return Optional.of(local);
        }

        SensorCacheEntry fromRedis = sensorRedisTemplate.opsForValue().get(REDIS_KEY_PREFIX + sensorEui);
        if (fromRedis != null) {
            sensorEuiLocalCache.put(sensorEui, fromRedis);
            return Optional.of(fromRedis);
        }

        Optional<SensorCacheEntry> fromDb = sensorRepository.findBySensorEui(sensorEui)
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
    public void populate(SensorCacheEntry entry) {
        sensorEuiLocalCache.put(entry.sensorEui(), entry);
        sensorRedisTemplate.opsForValue().set(REDIS_KEY_PREFIX + entry.sensorEui(), entry, REDIS_TTL);
    }

    /**
     * 캐시 항목을 Caffeine과 Redis 양쪽에서 제거함. 기기 정보가 변경/삭제됐을 때 사용함.
     *
     * @param sensorEui 제거할 기기의 고유 식별자
     */
    public void evict(String sensorEui) {
        sensorEuiLocalCache.invalidate(sensorEui);
        sensorRedisTemplate.delete(REDIS_KEY_PREFIX + sensorEui);
    }

    /**
     * {@code Sensor} 엔티티에서 캐시에 담을 최소 필드만 뽑아 {@link SensorCacheEntry}로 변환함.
     * gateway는 ACTUATOR 타입에서, location은 아직 공간에 배치되지 않은 기기에서 각각 null일 수 있으므로
     * 반드시 null 가드가 필요함 — 특히 Auto-Provisioning으로 새로 생성된 기기는 항상 location이 null이라,
     * 가드 없이 접근하면 첫 패킷부터 NPE가 발생하고 그 예외가 MQTT 연결까지 끊게 됨.
     *
     * @param sensor 변환할 기기 엔티티
     * @return 캐시용 최소 필드 레코드
     */
    private SensorCacheEntry toCacheEntry(Sensor sensor) {
        return new SensorCacheEntry(
                sensor.getSensorId(),
                sensor.getSensorEui(),
                sensor.getGateway().getGatewayId(),
                sensor.getLocation().getLocationId()
        );
    }
}