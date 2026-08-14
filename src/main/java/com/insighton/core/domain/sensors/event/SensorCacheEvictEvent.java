package com.insighton.core.domain.sensors.event;

// 센서의 위치 detach 등으로 캐시 무효화가 필요할 때 발행하는 이벤트 (트랜잭션 커밋 후 소비됨)
public record SensorCacheEvictEvent(String sensorEui) {}
