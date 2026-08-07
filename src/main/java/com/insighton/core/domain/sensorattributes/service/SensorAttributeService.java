package com.insighton.core.domain.sensorattributes.service;

import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionCreateRequest;
import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionResponse;
import com.insighton.core.domain.sensorattributes.dto.SensorAttributeResponse;

import java.util.List;

/**
 * 장치 속성(SensorAttribute) 데이터 서빙 및 액추에이터 제어 상태 갱신 인터페이스.
 */
public interface SensorAttributeService {

    // 메트릭 정의 추가
    void createMetricDefinition(MetricDefinitionCreateRequest request);

    /**
     * 특정 기기에 정의된 모든 속성(메트릭) 목록을 조회합니다.
     *
     * @param sensorId 조회할 기기 ID
     * @return 기기의 메트릭 정보 및 상태값이 포함된 DTO 리스트
     */
    List<SensorAttributeResponse> getAllAttributeBySensorId(Long userId , Long sensorId);

    /**
     * 외부 입력 패킷이 유효한 기기 속성인지 검증합니다.
     *
     * @param sensorId 장치 ID
     * @param metricKey 메트릭 키
     * @return 유효성 여부
     */
    boolean isValidSensorAttribute(Long sensorId, String metricKey);

    List<MetricDefinitionResponse> getAllMetricDefinitions();

    void deleteAttribute(Long userId, Long sensorId, String metricKey);

}