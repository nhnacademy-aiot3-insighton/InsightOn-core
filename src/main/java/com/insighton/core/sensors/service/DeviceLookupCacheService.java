package com.insighton.core.sensors.service; // 디바이스 서비스 인터페이스 패키지 정의

import com.insighton.core.sensors.entity.DeviceCacheEntry; // 디바이스 캐시 엔트리 DTO 임포트
import java.util.Optional; // Null 안전성을 위한 Optional 객체 임포트

// MQTT 메시지 패킷 수신 시 고속 조회를 위한 인메모리 캐시 관리 인터페이스
public interface DeviceLookupCacheService {

    // DevEUI 기반으로 인메모리 캐시에 적재된 디바이스 정보 조회 (캐시 히트/미스 검증용)
    Optional<DeviceCacheEntry> getByDeviceEui(String deviceEui);

    // Auto-Provisioning(자동 등록) 수행 직후 새로 생성된 디바이스 캐시 정보를 캐시 스토어에 적재
    void populate(DeviceCacheEntry cacheEntry);

    // 디바이스의 설치 위치(locationId) 변경 시 캐시에 등록된 기존 위치 정보를 신규 값으로 갱신
    void updateLocation(String deviceEui, Long newLocationId);

    // 디바이스 삭제 처리 시 해당 DevEUI 기반 캐시 데이터 제거
    void evict(String deviceEui);

    // 시스템 재시작 또는 캐시 리셋 필요 시 저장된 전체 캐시 데이터 초기화
    void clear();
}