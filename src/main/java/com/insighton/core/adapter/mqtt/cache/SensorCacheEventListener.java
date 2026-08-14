package com.insighton.core.adapter.mqtt.cache;

import com.insighton.core.domain.sensors.event.SensorCacheEvictEvent;
import com.insighton.core.domain.sensors.event.SensorCacheSyncEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SensorCacheEventListener {

    private final SensorLookupCacheService sensorLookupCacheService;

    // PG 트랜잭션이 실제로 커밋된 뒤에만 실행됨 - 트랜잭션이 롤백되면 이 메서드 자체가 안 불려서
    // "PG는 롤백됐는데 캐시엔 바뀐 값이 남아있는" 정합성 깨짐을 막아줌
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSensorCacheSync(SensorCacheSyncEvent event) {
        sensorLookupCacheService.populate(event.cacheEntry());
    }

    // 삭재
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSensorCacheEvict(SensorCacheEvictEvent event) {
        sensorLookupCacheService.evict(event.sensorEui());
    }
}
