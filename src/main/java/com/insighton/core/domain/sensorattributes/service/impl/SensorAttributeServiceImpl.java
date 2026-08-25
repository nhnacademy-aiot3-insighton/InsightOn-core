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
import java.util.Map;
import java.util.stream.Collectors;

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
        sensorRepository.findById(sensorId)
                .orElseThrow(() -> new SensorNotFoundException(sensorId));

        // 2. DB에서 장치 속성 목록 조회
        List<SensorAttribute> attributes = attributeRepository.findBySensorSensorId(sensorId);

        // 3. metricKey만으로는 한글명/단위를 모르니, metric_definitions에서 표준 정의를 배치로 한 번에 조회 (N+1 방지)
        // Map 키는 소문자로 통일 - 리포지토리 쪽 대소문자 무시 비교와 맞춰야 아래 조회에서 안 놓침
        Map<String, MetricDefinition> definitionsByKey = metricDefinitionRepository
                .findByMetricKeyInIgnoreCase(attributes.stream()
                        .map(attr -> attr.getMetricKey().toLowerCase())
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(m -> m.getMetricKey().toLowerCase(), m -> m));

        // 4. 조회된 정의와 매핑하여 DTO로 변환
        return attributes.stream()
                .map(attr -> {
                    MetricDefinition metricDefinition = definitionsByKey.get(attr.getMetricKey().toLowerCase());
                    if (metricDefinition == null) {
                        throw new MetricKeyNotFoundException(attr.getMetricKey());
                    }
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
                .toList(); // 특정 센서와 무관한 전체 조회
    }

    @Override
    @Transactional
    public void deleteAttribute(Long sensorId, String metricKey) {
        attributeRepository.findBySensorSensorIdAndMetricKey(sensorId, metricKey)
                .orElseThrow(() -> new MetricKeyNotFoundException(metricKey));


        attributeRepository.deleteBySensorSensorIdAndMetricKey(sensorId, metricKey);
    }

}