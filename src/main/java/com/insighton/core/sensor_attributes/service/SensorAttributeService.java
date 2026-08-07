package com.insighton.core.sensor_attributes.service;

import com.insighton.core.sensor_attributes.dto.SensorAttributeResponse;

import java.util.List;

/**
 * 장치 속성(SensorAttribute) 데이터 서빙 및 액추에이터 제어 상태 갱신 인터페이스.
 */
public interface SensorAttributeService {

    /**
     * 특정 기기에 정의된 모든 속성(메트릭) 목록을 조회합니다.
     *
     * @param sensorId 조회할 기기 ID
     * @return 기기의 메트릭 정보 및 상태값이 포함된 DTO 리스트
     */
    List<SensorAttributeResponse> getAllAttributeBySensorId(Long userId , Long sensorId);

    /**
     * 액추에이터 제어 명령 실행 시 최신 상태값을 갱신합니다. (권한 체크 필요 - userId 추가)
     *
     * @param userId 요청자 ID
     * @param sensorId 대상 액추에이터 기기 ID
     * @param metricKey 변경 대상 메트릭 키 (ex. "power_status", "ac_mode")
     * @param newValue 변경하고자 하는 상태/수치값 (ex. "ON", "OFF", "24")
     */
    void updateActuatorValue(Long userId, Long sensorId, String metricKey, String newValue);

    /**
     * 외부 입력 패킷이 유효한 기기 속성인지 검증합니다.
     *
     * @param sensorId 장치 ID
     * @param metricKey 메트릭 키
     * @return 유효성 여부
     */
    boolean isValidSensorAttribute(Long sensorId, String metricKey);
}