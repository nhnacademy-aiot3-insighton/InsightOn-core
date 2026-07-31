package com.insighton.core.mqtt.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 게이트웨이 PK로 소속 groupId를 조회하는 로컬(Caffeine) 캐시.
 * 패킷 처리 핫패스에서 groupId가 필요할 때마다 DB를 안 타려고 두는 캐시로,
 * 게이트웨이의 MQTT 연결이 등록될 때(선호/백업 소유 무관) 채워짐.
 */
@Component
public class GatewayGroupMappingCache {

    private final Cache<Long, Long> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .build();

    /**
     * 게이트웨이-그룹 매핑을 캐시에 저장함. 게이트웨이의 MQTT 연결을 등록하는 시점
     * ({@code GatewayMqttConnectionReconciler.tryRegister()})에 호출됨.
     *
     * @param gatewayId 매핑을 저장할 게이트웨이 PK
     * @param groupId   해당 게이트웨이가 소속된 그룹 PK
     */
    public void put(Long gatewayId, Long groupId) {
        cache.put(gatewayId, groupId);
    }

    /**
     * 게이트웨이 PK로 소속 groupId를 조회함. MQTT 패킷 처리 시 {@code GatewayPacketInboundHandler}가
     * 기기의 groupId를 확정하기 위해 호출함(groupId는 기기가 아니라 게이트웨이 소속이라
     * devEui가 아닌 gatewayId로 조회함).
     *
     * @param gatewayId 조회할 게이트웨이 PK
     * @return 캐시에 있으면 groupId, 없으면 빈 Optional
     */
    public Optional<Long> get(Long gatewayId) {
        return Optional.ofNullable(cache.getIfPresent(gatewayId));
    }

    /**
     * 캐시에서 해당 게이트웨이의 매핑을 제거함. 게이트웨이가 삭제되거나 소속 그룹이
     * 바뀌었을 때 stale 데이터가 남지 않도록 호출함.
     *
     * @param gatewayId 제거할 게이트웨이 PK
     */
    public void evict(Long gatewayId) {
        cache.invalidate(gatewayId);
    }
}
