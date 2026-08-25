package com.insighton.core.domain.sensors.service;

import com.insighton.core.adapter.mqtt.cache.dto.SensorCacheEntry;
import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.dto.SensorUpdateRequest;

import java.util.List;
import java.util.Set;

public interface SensorService {

    // MQTT 패킷 수신 시 캐시 미스가 발생하면 호출되어 센서를 자동 생성하고 캐시 엔트리를 반환하는 메서드
    SensorCacheEntry autoProvision(Long gatewayId, Long groupId, String sensorEui, String sensorName, Set<String> metricKeys);

    //단일 장치 상세 조회
    SensorResponse getSensorById(Long sensorId);

    // 업데이트 - locationId가 있으면 위치도 함께 변경 (프론트가 드롭다운으로 선택해서 넘겨줌)
    void updateSensor(Long sensorId, SensorUpdateRequest request);


    // 단일 장치 삭제 (권한 체크 필요 - userId 추가)
    void deleteSensor(Long sensorId);

    // 전체 장치 삭제 (권한 체크 필요 - userId, 삭제 대상 groupId 추가)
    void deleteAll(Long groupId);

    // 조건별 장치 검색 (id/eui/locationId/sensorName 중 있는 조건만 AND로 조합)
    List<SensorResponse> searchSensors(Long groupId, Long id, String eui, SensorUpdateRequest request);

    // 장소 미배정(location이 null인) 센서 목록 조회 - 자동 등록됐지만 아직 설치 위치를 안 정한 센서 파악용
    List<SensorResponse> getUnassignedSensors(Long groupId);

    Long getSensorGroupId(Long sensorId);

    /**
     * location 삭제를 위해 만들었습니다
     *
     * @param groupId    location이 속해있는 group ID
     * @param locationId device와 연결되어있는 location
     */
    void detachLocationFromSensors(Long groupId, Long locationId);
}
