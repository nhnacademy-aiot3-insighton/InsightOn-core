package com.insighton.core.mqtt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class GatewayGroupMappingCache {

    private final Cache<Long, Long> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .build();

    /**
     * 게이트웨이와 그룹 간 매핑을 캐시에 저장하거나 갱신합니다.
     *
     * @param gatewayId 매핑할 게이트웨이 식별자
     * @param groupId 게이트웨이에 연결할 그룹 식별자
     */
    public void put(Long gatewayId, Long groupId) {
        cache.put(gatewayId, groupId);
    }

    /**
     * 게이트웨이에 연결된 그룹 식별자를 조회합니다.
     *
     * @param gatewayId 조회할 게이트웨이 식별자
     * @return 캐시에 저장된 그룹 식별자이며, 매핑이 없으면 빈 {@code Optional}
     */
    public Optional<Long> get(Long gatewayId) {
        return Optional.ofNullable(cache.getIfPresent(gatewayId));
    }

    /**
     * 게이트웨이 ID에 해당하는 그룹 매핑을 캐시에서 삭제합니다.
     *
     * @param gatewayId 삭제할 매핑의 게이트웨이 ID
     */
    public void evict(Long gatewayId) {
        cache.invalidate(gatewayId);
    }
}
