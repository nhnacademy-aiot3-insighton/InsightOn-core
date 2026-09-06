package com.insighton.core.adapter.mqtt.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.insighton.core.domain.gateway.MqttGatewayConnectionInfo;
import org.junit.jupiter.api.Test;

/**
 * 로컬 Caffeine/ConcurrentHashMap 기반 캐시 두 종은 로직이 단순한 CRUD 래퍼라 한 파일에서 함께 검증.
 */
class GatewayCachesTest {

    @Test
    void GatewayConnectionInfoCache_저장_조회_삭제() {
        GatewayConnectionInfoCache cache = new GatewayConnectionInfoCache();
        MqttGatewayConnectionInfo info = new MqttGatewayConnectionInfo(1L, "insighton-1",
                new String[]{"tcp://broker:1883"}, new String[]{"topic/+"}, null, null);

        cache.put(info);
        assertThat(cache.get(1L)).contains(info);
        assertThat(cache.getAll()).containsKey(1L);

        cache.remove(1L);
        assertThat(cache.get(1L)).isEmpty();
    }

    @Test
    void GatewayConnectionInfoCache_없는_키는_빈_Optional() {
        GatewayConnectionInfoCache cache = new GatewayConnectionInfoCache();

        assertThat(cache.get(999L)).isEmpty();
    }

    @Test
    void GatewayGroupMappingCache_저장_조회_삭제() {
        GatewayGroupMappingCache cache = new GatewayGroupMappingCache();

        cache.put(1L, 100L);
        assertThat(cache.get(1L)).contains(100L);

        cache.evict(1L);
        assertThat(cache.get(1L)).isEmpty();
    }

    @Test
    void GatewayGroupMappingCache_없는_키는_빈_Optional() {
        GatewayGroupMappingCache cache = new GatewayGroupMappingCache();

        assertThat(cache.get(999L)).isEmpty();
    }
}
