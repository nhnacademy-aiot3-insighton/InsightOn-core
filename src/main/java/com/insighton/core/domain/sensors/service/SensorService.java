package com.insighton.core.domain.sensors.service;

import com.insighton.core.adapter.mqtt.cache.dto.SensorCacheEntry;
import com.insighton.core.domain.sensors.dto.SensorResponse;

import java.util.List;
import java.util.Set;

public interface SensorService {

    // MQTT 패킷 수신 시 캐시 미스가 발생하면 호출되어 센서를 자동 생성하고 캐시 엔트리를 반환하는 메서드
    SensorCacheEntry autoProvision(Long gatewayId, Long groupId, String sensorEui, String sensorName, Set<String> metricKeys);

    //단일 장치 상세 조회
    SensorResponse getSensorById(Long userId, Long sensorId);

    // 위치 수정 (권한 체크 필요 - userId 추가)
    void updateSensorLocation(Long userId, Long sensorId, Long newLocationId);

    // 이름 수정 (권한 체크 필요 - userId 추가)
    void updateSensorName(Long userId, Long sensorId, String newSensorName);

    // 업데이트
    void updateSensor(Long userId, Long sensorId, Long newLocationId, String newSensorName);

    // MQTT/패킷 수신 시 마지막 통신 시각 갱신
    void handlePacketReceived(String sensorEui);

    // 단일 장치 삭제 (권한 체크 필요 - userId 추가)
    void deleteSensor(Long userId, Long sensorId);

    // 전체 장치 삭제 (권한 체크 필요 - userId, 삭제 대상 groupId 추가)
    void deleteAll(Long userId, Long groupId);

    // 조건별 장치 검색 (단순 분기)
    List<SensorResponse> searchSensors(Long userId, Long groupId, Long id, String eui,
                                       Long locationId, String sensorName);

    /**
     * location 삭제를 위해 만들었습니다
     *
     * @param groupId    location이 속해있는 group ID
     * @param locationId device와 연결되어있는 location
     */
    void detachLocationFromSensors(Long groupId, Long locationId);
}