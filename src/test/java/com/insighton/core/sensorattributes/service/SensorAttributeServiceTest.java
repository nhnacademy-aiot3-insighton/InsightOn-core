package com.insighton.core.sensorattributes.service;


import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionCreateRequest;
import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionResponse;
import com.insighton.core.domain.sensorattributes.dto.SensorAttributeResponse;
import com.insighton.core.domain.sensorattributes.entity.MetricDefinition;
import com.insighton.core.domain.sensorattributes.entity.SensorAttribute;
import com.insighton.core.domain.sensorattributes.exception.MetricKeyAlreadyExistsException;
import com.insighton.core.domain.sensorattributes.exception.MetricKeyNotFoundException;
import com.insighton.core.domain.sensorattributes.repository.MetricDefinitionRepository;
import com.insighton.core.domain.sensorattributes.repository.SensorAttributeRepository;
import com.insighton.core.domain.sensorattributes.service.impl.SensorAttributeServiceImpl;
import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.sensors.repository.SensorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 권한 체크(그룹 멤버십 검증)는 usecase.sensorattribute 패키지로 이동해서, 이 클래스는
// 순수 영속성 로직만 검증함. isValidSensorAttribute는 실사용처가 없어 서비스에서 제거됐으므로
// 관련 테스트도 같이 삭제함.
@ExtendWith(MockitoExtension.class)
class SensorAttributeServiceTest {

    @Mock private SensorAttributeRepository attributeRepository;
    @Mock private SensorRepository sensorRepository;
    @Mock private MetricDefinitionRepository metricDefinitionRepository;

    @InjectMocks
    private SensorAttributeServiceImpl attributeService;

    @Test
    @DisplayName("getAllAttributeBySensorId - 센서 없으면 예외")
    void 목록조회_센서없음() {
        given(sensorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(SensorNotFoundException.class,
                () -> attributeService.getAllAttributeBySensorId(999L));
    }

    @Test
    @DisplayName("getAllAttributeBySensorId - 정상 조회시 메트릭 정의와 매핑되어 반환")
    void 목록조회_성공() {
        Sensor sensor = mock(Sensor.class);
        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));

        SensorAttribute attribute = SensorAttribute.builder().sensor(sensor).metricKey("co2").build();
        given(attributeRepository.findBySensorSensorId(1L)).willReturn(List.of(attribute));

        MetricDefinition definition = MetricDefinition.builder()
                .metricKey("co2").metricName("이산화탄소").unit("ppm").build();
        given(metricDefinitionRepository.findByMetricKeyIgnoreCase("co2")).willReturn(Optional.of(definition));

        List<SensorAttributeResponse> result = attributeService.getAllAttributeBySensorId(1L);

        assertThat(result).containsExactly(new SensorAttributeResponse("co2", "이산화탄소", "ppm"));
    }

    @Test
    @DisplayName("getAllAttributeBySensorId - 속성의 metricKey가 카탈로그에 없으면 MetricKeyNotFoundException")
    void 목록조회_메트릭정의없음() {
        Sensor sensor = mock(Sensor.class);
        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));

        SensorAttribute attribute = SensorAttribute.builder().sensor(sensor).metricKey("deprecated_key").build();
        given(attributeRepository.findBySensorSensorId(1L)).willReturn(List.of(attribute));
        given(metricDefinitionRepository.findByMetricKeyIgnoreCase("deprecated_key")).willReturn(Optional.empty());

        assertThrows(MetricKeyNotFoundException.class,
                () -> attributeService.getAllAttributeBySensorId(1L));
    }

    @Test
    @DisplayName("createMetricDefinition - 정상 생성")
    void 메트릭생성_성공() {
        given(metricDefinitionRepository.findByMetricKeyIgnoreCase("pm2.5")).willReturn(Optional.empty());

        attributeService.createMetricDefinition(new MetricDefinitionCreateRequest("pm2.5", "미세먼지", "㎍/㎥"));

        verify(metricDefinitionRepository).save(any(MetricDefinition.class));
    }

    @Test
    @DisplayName("createMetricDefinition - 이미 등록된 metricKey면 예외, 저장 안 함")
    void 메트릭생성_중복키_거부() {
        given(metricDefinitionRepository.findByMetricKeyIgnoreCase("co2"))
                .willReturn(Optional.of(MetricDefinition.builder().metricKey("co2").metricName("이산화탄소").build()));

        assertThrows(MetricKeyAlreadyExistsException.class,
                () -> attributeService.createMetricDefinition(new MetricDefinitionCreateRequest("co2", "이산화탄소", "ppm")));

        verify(metricDefinitionRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllMetricDefinitions - 전체 메트릭 정의 목록 반환")
    void 메트릭목록조회_성공() {
        MetricDefinition definition = MetricDefinition.builder()
                .metricKey("co2").metricName("이산화탄소").unit("ppm").build();
        given(metricDefinitionRepository.findAll()).willReturn(List.of(definition));

        List<MetricDefinitionResponse> result = attributeService.getAllMetricDefinitions();

        assertThat(result).containsExactly(MetricDefinitionResponse.from(definition));
    }

    @Test
    @DisplayName("deleteAttribute - 정상 삭제")
    void 속성삭제_성공() {
        Sensor sensor = mock(Sensor.class);
        SensorAttribute attribute = SensorAttribute.builder().sensor(sensor).metricKey("co2").build();
        given(attributeRepository.findBySensorSensorIdAndMetricKey(1L, "co2")).willReturn(Optional.of(attribute));

        attributeService.deleteAttribute(1L, "co2");

        verify(attributeRepository).deleteBySensorSensorIdAndMetricKey(1L, "co2");
    }

    @Test
    @DisplayName("deleteAttribute - 존재하지 않는 속성이면 예외, 삭제 안 함")
    void 속성삭제_없는속성_거부() {
        given(attributeRepository.findBySensorSensorIdAndMetricKey(1L, "unknown")).willReturn(Optional.empty());

        assertThrows(MetricKeyNotFoundException.class,
                () -> attributeService.deleteAttribute(1L, "unknown"));

        verify(attributeRepository, never()).deleteBySensorSensorIdAndMetricKey(any(), any());
    }
}
