package com.insighton.core.mqtt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import org.springframework.stereotype.Component;

// TODO: 이 캐시를 채우는 로직은 GatewayRepository 완성 후 Warm-up에서 연결
@Component
public class GatewayGroupMappingCache {

    private final Cache<Long, Long> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .build();

    public void put(Long gatewayId, Long groupId) {
        cache.put(gatewayId, groupId);
    }

    public Optional<Long> get(Long gatewayId) {
        return Optional.ofNullable(cache.getIfPresent(gatewayId));
    }

    public void evict(Long gatewayId) {
        cache.invalidate(gatewayId);
    }
}
