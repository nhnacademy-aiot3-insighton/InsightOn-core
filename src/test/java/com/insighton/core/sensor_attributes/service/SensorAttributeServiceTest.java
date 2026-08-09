package com.insighton.core.sensor_attributes.service;

import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.entity.GroupMember.GroupRole;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionCreateRequest;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensorAttributeServiceTest {

    @Mock private SensorAttributeRepository attributeRepository;
    @Mock private SensorRepository sensorRepository;
    @Mock private GroupMemberService groupMemberService;
    @Mock private MetricDefinitionRepository metricDefinitionRepository;

    @InjectMocks
    private SensorAttributeServiceImpl attributeService;

    private Sensor sensor(Long groupId) {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(groupId);
        return Sensor.builder().sensorId(1L).group(group).build();
    }

    @Test
    @DisplayName("getAllAttributeBySensorId - 정상 조회")
    void 조회_성공() {
        Sensor sensor = sensor(5L);
        given(sensorRepository.findById(1L)).willReturn(Optional.of(sensor));
        given(attributeRepository.findBySensorSensorId(1L))
                .willReturn(List.of(SensorAttribute.builder().metricKey("co2").build()));
        given(metricDefinitionRepository.findByMetricKeyIgnoreCase("co2"))
                .willReturn(Optional.of(MetricDefinition.builder().metricKey("co2").metricName("이산화탄소").unit("ppm").build()));

        var result = attributeService.getAllAttributeBySensorId(1L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).displayName()).isEqualTo("이산화탄소");
    }

    @Test
    @DisplayName("getAllAttributeBySensorId - 없는 센서면 SensorNotFoundException")
    void 조회_없는센서() {
        given(sensorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(SensorNotFoundException.class,
                () -> attributeService.getAllAttributeBySensorId(1L, 999L));
    }

    @Test
    @DisplayName("deleteAttribute - 매니저 권한 확인 후 삭제")
    void 삭제_성공() {
        Sensor sensor = sensor(5L);
        SensorAttribute attribute = SensorAttribute.builder().metricKey("co2").sensor(sensor).build();

        given(attributeRepository.findBySensorSensorIdAndMetricKey(1L, "co2")).willReturn(Optional.of(attribute));
        given(groupMemberService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MANAGER).build());

        attributeService.deleteAttribute(1L, 1L, "co2");

        verify(attributeRepository).deleteBySensorSensorIdAndMetricKey(1L, "co2");
    }

    @Test
    @DisplayName("deleteAttribute - MEMBER 권한이면 거부")
    void 삭제_권한없음() {
        Sensor sensor = sensor(5L);
        SensorAttribute attribute = SensorAttribute.builder().metricKey("co2").sensor(sensor).build();

        given(attributeRepository.findBySensorSensorIdAndMetricKey(1L, "co2")).willReturn(Optional.of(attribute));
        given(groupMemberService.validateGroupMembers(5L, 1L))
                .willReturn(GroupMember.builder().userId(1L).groupRole(GroupRole.MEMBER).build());

        assertThrows(NoPermissionException.class,
                () -> attributeService.deleteAttribute(1L, 1L, "co2"));

        verify(attributeRepository, never()).deleteBySensorSensorIdAndMetricKey(any(), any());
    }

    @Test
    @DisplayName("deleteAttribute - 존재하지 않는 metricKey면 MetricKeyNotFoundException")
    void 삭제_없는키() {
        given(attributeRepository.findBySensorSensorIdAndMetricKey(1L, "unknown")).willReturn(Optional.empty());

        assertThrows(MetricKeyNotFoundException.class,
                () -> attributeService.deleteAttribute(1L, 1L, "unknown"));
    }

    @Test
    @DisplayName("createMetricDefinition - 정상 등록")
    void 메트릭정의_등록_성공() {
        MetricDefinitionCreateRequest request = new MetricDefinitionCreateRequest("pm2.5", "미세먼지", "㎍/㎥");
        given(metricDefinitionRepository.findByMetricKeyIgnoreCase("pm2.5")).willReturn(Optional.empty());

        attributeService.createMetricDefinition(request);

        verify(metricDefinitionRepository).save(any(MetricDefinition.class));
    }

    @Test
    @DisplayName("createMetricDefinition - 이미 존재하면 MetricKeyAlreadyExistsException")
    void 메트릭정의_중복_거부() {
        MetricDefinitionCreateRequest request = new MetricDefinitionCreateRequest("co2", "이산화탄소", "ppm");
        given(metricDefinitionRepository.findByMetricKeyIgnoreCase("co2"))
                .willReturn(Optional.of(MetricDefinition.builder().metricKey("co2").build()));

        assertThrows(MetricKeyAlreadyExistsException.class,
                () -> attributeService.createMetricDefinition(request));

        verify(metricDefinitionRepository, never()).save(any());
    }

    @Test
    @DisplayName("isValidSensorAttribute - 대소문자 달라도 정규화되어 조회됨")
    void 유효성검증_대소문자_정규화() {
        given(metricDefinitionRepository.findByMetricKeyIgnoreCase("CO2"))
                .willReturn(Optional.of(MetricDefinition.builder().metricKey("co2").build()));
        given(attributeRepository.existsBySensorSensorIdAndMetricKey(1L, "co2")).willReturn(true);

        assertThat(attributeService.isValidSensorAttribute(1L, "CO2")).isTrue();
    }

    @Test
    @DisplayName("isValidSensorAttribute - 등록되지 않은 키는 예외 없이 false")
    void 유효성검증_없는키_false() {
        given(metricDefinitionRepository.findByMetricKeyIgnoreCase("unknown")).willReturn(Optional.empty());

        assertThat(attributeService.isValidSensorAttribute(1L, "unknown")).isFalse();
    }
}