package com.insighton.core.domain.sensors.event;

import com.insighton.core.adapter.mqtt.cache.dto.SensorCacheEntry;

// 센서 위치 변경 등으로 캐시(Caffeine/Redis)를 갱신해야 할 때 발행하는 이벤트 (트랜잭션 커밋 후 소비됨)
public record SensorCacheSyncEvent(
        SensorCacheEntry cacheEntry
) {}
