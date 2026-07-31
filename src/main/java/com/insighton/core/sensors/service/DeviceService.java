
package com.insighton.core.sensors.service;

import com.insighton.core.sensors.dto.DeviceRequest;
import com.insighton.core.sensors.dto.DeviceResponse;
import com.insighton.core.sensors.entity.DeviceCacheEntry;

import java.util.List;
import java.util.Set;

public interface DeviceService {

    // MQTT 패킷 수신 시 캐시 미스가 발생하면 호출되어 센서를 자동 생성하고 캐시 엔트리를 반환하는 메서드
    DeviceCacheEntry autoProvision(Long gatewayId, Long groupId, String deviceEui, String deviceName, Set<String> metricKeys);

    /**
     * 장치 생성 및 등록
     */
//    Long createActuator(DeviceRequest requestDto);

    /**
     * 단일 장치 상세 조회
     */
    DeviceResponse getDeviceById(Long deviceId);

    // 위치 수정
    void updateDeviceLocation(Long deviceId, Long newLocationId);

    // 이름 수정 (추가)
    void updateDeviceName(Long deviceId, String newDeviceName);

    /**
     * MQTT/패킷 수신 시 마지막 통신 시각 갱신
     */
    void handlePacketReceived(String deviceEui);

    /**
     * 단일 장치 삭제
     */
    void deleteDevice(Long deviceId);

    /**
     * 전체 장치 삭제
     */
    void deleteAll();

    /**
     * 조건별 장치 검색 (단순 분기)
     */
    List<DeviceResponse> searchDevices(Long id, String eui, Long locationId, Long gatewayId, String deviceName);
}