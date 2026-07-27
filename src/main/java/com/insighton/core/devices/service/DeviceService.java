
package com.insighton.core.devices.service;

import com.insighton.core.devices.dto.DeviceRequest;
import com.insighton.core.devices.dto.DeviceResponse;

import java.util.List;

public interface DeviceService {

    /**
     * 장치 생성 및 등록
     */
    Long createDevice(DeviceRequest requestDto);

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