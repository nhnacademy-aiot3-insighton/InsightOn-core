package com.insighton.core.domain.sensorattributes.service.impl;

import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionCreateRequest;
import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionResponse;
import com.insighton.core.domain.sensorattributes.dto.SensorAttributeResponse;
import com.insighton.core.domain.sensorattributes.entity.MetricDefinition;
import com.insighton.core.domain.sensorattributes.entity.SensorAttribute;
import com.insighton.core.domain.sensorattributes.exception.MetricKeyAlreadyExistsException;
import com.insighton.core.domain.sensorattributes.exception.MetricKeyNotFoundException;
import com.insighton.core.domain.sensorattributes.repository.MetricDefinitionRepository;
import com.insighton.core.domain.sensorattributes.repository.SensorAttributeRepository;
import com.insighton.core.domain.sensorattributes.service.SensorAttributeService;
import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.sensors.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorAttributeServiceImpl implements SensorAttributeService {

    private final SensorAttributeRepository attributeRepository; // 속성 조회/저장
    private final SensorRepository sensorRepository; // 소속 센서 조회
    private final MetricDefinitionRepository metricDefinitionRepository;



    // 추가적인 메트릭 정의가 있을시 추가
    @Override
    @Transactional
    public void createMetricDefinition(MetricDefinitionCreateRequest request) {

        if(metricDefinitionRepository.findByMetricKeyIgnoreCase(request.metricKey()).isPresent()){
            throw new MetricKeyAlreadyExistsException(request.metricKey());
        }

        MetricDefinition metricDefinition = MetricDefinition.builder()
                .metricKey(request.metricKey())
                .metricName(request.metricName())
                .unit(request.unit())
                .build();

        metricDefinitionRepository.save(metricDefinition);
    }

    @Override
    public List<SensorAttributeResponse> getAllAttributeBySensorId(Long sensorId) {

        // 1. 해당 장치의 존재 유무 검증
        Sensor entity = sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId));

        // 2. DB에서 장치 속성 목록 조회 후 Enum 정보를 매핑하여 DTO로 변환
        return attributeRepository.findBySensorSensorId(sensorId)
                .stream()
                .map(attr -> {
                    // Enum에서 메트릭 표준 정의(한글 명칭, 단위) 바인딩
                    MetricDefinition metricDefinition = metricDefinitionRepository
                            .findByMetricKeyIgnoreCase(attr.getMetricKey())
                            .orElseThrow(() -> new MetricKeyNotFoundException(attr.getMetricKey()));

                    return new SensorAttributeResponse(
                            attr.getMetricKey(),
                            metricDefinition.getMetricName(),
                            metricDefinition.getUnit()
                    );
                }).toList();
    }


    // 매트릭 정의 목록 조회
    @Override
    public List<MetricDefinitionResponse> getAllMetricDefinitions() {
        return metricDefinitionRepository.findAll().stream()
                .map(MetricDefinitionResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAttribute(Long sensorId, String metricKey) {
        SensorAttribute attribute = attributeRepository.findBySensorSensorIdAndMetricKey(sensorId, metricKey)
                .orElseThrow(() -> new MetricKeyNotFoundException(metricKey));

        attributeRepository.deleteBySensorSensorIdAndMetricKey(sensorId, metricKey);
    }

}