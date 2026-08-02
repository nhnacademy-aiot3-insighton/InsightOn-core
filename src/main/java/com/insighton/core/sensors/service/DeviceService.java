package com.insighton.core.sensors.service;

import com.insighton.core.mqtt.cache.dto.DeviceCacheEntry;
import com.insighton.core.sensors.dto.DeviceResponse;

import java.util.List;
import java.util.Set;

public interface DeviceService {

    // MQTT 패킷 수신 시 캐시 미스가 발생하면 호출되어 센서를 자동 생성하고 캐시 엔트리를 반환하는 메서드
    DeviceCacheEntry autoProvision(Long gatewayId, Long groupId, String deviceEui, String deviceName, Set<String> metricKeys);

    //단일 장치 상세 조회
    DeviceResponse getDeviceById(Long userId, Long deviceId);

    // 위치 수정 (권한 체크 필요 - userId 추가)
//    void updateDeviceLocation(Long userId, Long deviceId, Long newLocationId);

    // 이름 수정 (권한 체크 필요 - userId 추가)
    void updateDeviceName(Long userId, Long deviceId, String newDeviceName);

    // MQTT/패킷 수신 시 마지막 통신 시각 갱신
    void handlePacketReceived(String deviceEui);

    // 단일 장치 삭제 (권한 체크 필요 - userId 추가)
    void deleteDevice(Long userId, Long deviceId);

    // 전체 장치 삭제 (권한 체크 필요 - userId, 삭제 대상 groupId 추가)
    void deleteAll(Long userId, Long groupId);

    // 조건별 장치 검색 (단순 분기)
    List<DeviceResponse> searchDevices(Long userId, Long groupId, Long id, String eui,
                                       Long locationId, Long gatewayId, String deviceName);
}