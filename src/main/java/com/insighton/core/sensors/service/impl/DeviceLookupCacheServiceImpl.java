package com.insighton.core.sensors.service.impl; // 디바이스 서비스 구현체 패키지 정의

import com.insighton.core.sensors.entity.DeviceCacheEntry; // 디바이스 캐시 엔트리 DTO 임포트
import com.insighton.core.sensors.service.DeviceLookupCacheService; // 캐시 서비스 인터페이스 임포트
import org.springframework.stereotype.Service; // 스프링 서비스 빈 등록 어노테이션 임포트

import java.util.Map; // Java Map 인터페이스 임포트
import java.util.Optional; // Optional 임포트
import java.util.concurrent.ConcurrentHashMap; // 동시성 보장 ConcurrentHashMap 임포트

@Service // 스프링 컨테이너에 서비스 빈(Bean)으로 등록
public class DeviceLookupCacheServiceImpl implements DeviceLookupCacheService {

    // 멀티스레드 환경에서 동시성 문제 없이 안전하게 캐시 데이터를 관리하기 위한 저장소 (Key: deviceEui)
    private final Map<String, DeviceCacheEntry> cacheStore = new ConcurrentHashMap<>();

    @Override // 인터페이스의 getByDeviceEui 메서드 구현
    public Optional<DeviceCacheEntry> getByDeviceEui(String deviceEui) {
        // 입력된 DevEUI 인자 값이 null인지 검증
        if (deviceEui == null) { 
            // null인 경우 조회 작업을 진행하지 않고 빈 Optional 객체 반환
            return Optional.empty(); 
        }
        // ConcurrentHashMap에서 DevEUI 키에 해당하는 캐시 데이터를 조회하여 Optional로 감싸서 반환
        return Optional.ofNullable(cacheStore.get(deviceEui)); 
    }

    @Override // 인터페이스의 populate 메서드 구현
    public void populate(DeviceCacheEntry cacheEntry) {
        // 전달받은 캐시 객체가 null이거나 내부의 deviceEui가 null인지 유효성 검사
        if (cacheEntry == null || cacheEntry.deviceEui() == null) { 
            // 유효하지 않은 데이터인 경우 캐시에 적재하지 않고 조기 리턴
            return; 
        }
        // deviceEui를 Key로 설정하여 인메모리 캐시 Map에 엔트리 저장
        cacheStore.put(cacheEntry.deviceEui(), cacheEntry); 
    }

    @Override // 인터페이스의 updateLocation 메서드 구현
    public void updateLocation(String deviceEui, Long newLocationId) {
        // 입력된 DevEUI가 null이거나 현재 캐시 스토어에 존재하지 않는 키인 경우 확인
        if (deviceEui == null || !cacheStore.containsKey(deviceEui)) { 
            // 대상 기기가 캐시에 없으면 위치 갱신 작업을 중단하고 조기 리턴
            return; 
        }

        // 캐시 스토어에서 기존 디바이스 캐시 정보를 꺼내옴
        DeviceCacheEntry existingEntry = cacheStore.get(deviceEui); 

        // 기존 데이터의 deviceId, deviceEui, gatewayId는 유지하면서 locationId만 신규 값으로 바꾼 새 객체 생성
        DeviceCacheEntry updatedEntry = new DeviceCacheEntry(
                existingEntry.deviceId(),  // 기존 디바이스 PK ID 유지
                existingEntry.deviceEui(), // 기존 DevEUI 유지
                existingEntry.gatewayId(), // 기존 게이트웨이 ID 유지
                newLocationId              // 새로 업데이트할 위치 ID 적용
        );

        // 새로 갱신된 캐시 엔트리로 캐시 스토어 덮어쓰기
        cacheStore.put(deviceEui, updatedEntry); 
    }

    @Override // 인터페이스의 evict 메서드 구현
    public void evict(String deviceEui) {
        // 삭제 대상 DevEUI 인자 값이 null이 아닌지 확인
        if (deviceEui != null) { 
            // 인메모리 캐시 Map에서 해당 DevEUI 키의 데이터 삭제
            cacheStore.remove(deviceEui); 
        }
    }

    @Override // 인터페이스의 clear 메서드 구현
    public void clear() {
        // 인메모리 캐시 Map에 저장된 모든 데이터 초기화
        cacheStore.clear(); 
    }
}