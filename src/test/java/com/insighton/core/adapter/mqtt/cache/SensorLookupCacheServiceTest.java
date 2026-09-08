package com.insighton.core.adapter.mqtt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.insighton.core.adapter.mqtt.cache.dto.SensorCacheEntry;
import com.insighton.core.domain.gateway.entity.Gateway;
import com.insighton.core.domain.gateway.entity.ProtocolType;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.repository.SensorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensorLookupCacheServiceTest {

    @Mock
    private Cache<String, SensorCacheEntry> sensorEuiLocalCache;

    @Mock
    private RedisTemplate<String, SensorCacheEntry> sensorRedisTemplate;

    @Mock
    private ValueOperations<String, SensorCacheEntry> valueOperations;

    @Mock
    private SensorRepository sensorRepository;

    @InjectMocks
    private SensorLookupCacheService sensorLookupCacheService;

    @Test
    @DisplayName("1계층 Caffeine 캐시 히트 케이스")
    void lookup_caffeineHit() {
        // given
        SensorCacheEntry entry = new SensorCacheEntry(1L, "EUI-123", 10L, 100L);
        given(sensorEuiLocalCache.getIfPresent("EUI-123")).willReturn(entry);

        // when
        Optional<SensorCacheEntry> result = sensorLookupCacheService.lookup("EUI-123");

        // then
        assertThat(result).contains(entry);
    }

    @Test
    @DisplayName("2계층 Redis 캐시 히트 케이스")
    void lookup_redisHit() {
        // given
        SensorCacheEntry entry = new SensorCacheEntry(1L, "EUI-123", 10L, 100L);
        given(sensorEuiLocalCache.getIfPresent("EUI-123")).willReturn(null);
        given(sensorRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("sensor:eui:EUI-123")).willReturn(entry);

        // when
        Optional<SensorCacheEntry> result = sensorLookupCacheService.lookup("EUI-123");

        // then
        assertThat(result).contains(entry);
        verify(sensorEuiLocalCache).put("EUI-123", entry);
    }

    @Test
    @DisplayName("3계층 DB 히트 케이스 및 캐시 populate 검증")
    void lookup_dbHit() {
        // given
        given(sensorEuiLocalCache.getIfPresent("EUI-123")).willReturn(null);
        given(sensorRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("sensor:eui:EUI-123")).willReturn(null);

        Gateway gateway = Gateway.builder().groupsId(100L).name("gw").protocolType(ProtocolType.MQTT).connectionConfig(Map.of()).build();
        ReflectionTestUtils.setField(gateway, "gatewayId", 10L);

        Location location = Location.builder().locationName("loc").build();
        ReflectionTestUtils.setField(location, "locationId", 100L);

        Sensor sensor = Sensor.builder()
                .sensorEui("EUI-123")
                .gateway(gateway)
                .location(location)
                .build();
        ReflectionTestUtils.setField(sensor, "sensorId", 1L);

        given(sensorRepository.findBySensorEui("EUI-123")).willReturn(Optional.of(sensor));

        // when
        Optional<SensorCacheEntry> result = sensorLookupCacheService.lookup("EUI-123");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().sensorEui()).isEqualTo("EUI-123");
        verify(sensorEuiLocalCache).put(eq("EUI-123"), any(SensorCacheEntry.class));
        verify(valueOperations).set(eq("sensor:eui:EUI-123"), any(SensorCacheEntry.class), any(Duration.class));
    }

    @Test
    @DisplayName("evict 호출 시 Caffeine 및 Redis 삭제 검증")
    void evict_success() {
        // when
        sensorLookupCacheService.evict("EUI-123");

        // then
        verify(sensorEuiLocalCache).invalidate("EUI-123");
        verify(sensorRedisTemplate).delete("sensor:eui:EUI-123");
    }
}
