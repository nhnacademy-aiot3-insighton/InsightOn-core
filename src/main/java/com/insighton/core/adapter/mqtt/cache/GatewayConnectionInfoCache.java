package com.insighton.core.adapter.mqtt.cache;

import com.insighton.core.domain.gateway.MqttGatewayConnectionInfo;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 게이트웨이 접속 정보를 게이트웨이 ID 기준으로 들고 있는 로컬 캐시.
 * Warm-up 시점에 채워지며, 이후 헬스체크/재등록 등에서 접속 정보를 다시 조회할 때 사용함.
 */
@Component
public class GatewayConnectionInfoCache {
    private final Map<Long, MqttGatewayConnectionInfo> cache = new ConcurrentHashMap<>();

    /**
     * 접속 정보를 게이트웨이 ID 기준으로 캐시에 저장함.
     *
     * @param connectionInfo 저장할 게이트웨이 접속 정보
     */
    public void put(MqttGatewayConnectionInfo connectionInfo) {
        cache.put(connectionInfo.gatewayId(), connectionInfo);
    }

    /**
     * 캐시에서 해당 게이트웨이의 접속 정보를 제거함.
     *
     * @param gatewayId 제거할 게이트웨이 PK
     */
    public void remove(Long gatewayId) {
        cache.remove(gatewayId);
    }

    /**
     * 게이트웨이 ID로 캐시된 접속 정보를 조회함.
     *
     * @param gatewayId 조회할 게이트웨이 PK
     * @return 캐시된 접속 정보, 없으면 빈 Optional
     */
    public Optional<MqttGatewayConnectionInfo> get(long gatewayId) {
        return Optional.ofNullable(cache.get(gatewayId));
    }

    /**
     * 현재 캐시된 전체 접속 정보의 방어적 복사본을 반환함.
     *
     * @return 게이트웨이 ID를 키로 하는 전체 접속 정보 맵
     */
    public Map<Long, MqttGatewayConnectionInfo> getAll() {
        return Map.copyOf(cache);
    }

}