package com.insighton.core.sensorattributes.service;


import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.sensorattributes.repository.SensorAttributeRepository;
import com.insighton.core.domain.sensorattributes.service.impl.SensorAttributeServiceImpl;
import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.sensors.repository.SensorRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@Disabled
@ExtendWith(MockitoExtension.class)
class SensorAttributeServiceTest {

    @Mock private SensorAttributeRepository attributeRepository;
    @Mock private SensorRepository sensorRepository;
    @Mock private GroupMemberService groupMemberService;

    @InjectMocks
    private SensorAttributeServiceImpl attributeService;

    private Sensor actuatorDevice(Long groupId) {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(groupId);
        return Sensor.builder().sensorId(1L).group(group).build();
    }

    private Sensor sensorDevice(Long groupId) {
        Group group = mock(Group.class);
        given(group.getGroupId()).willReturn(groupId);
        return Sensor.builder().sensorId(1L).group(group).build();
    }

    @Test
    @DisplayName("isValidSensorAttribute - 대소문자가 달라도 정규화되어 조회된다")
    void 유효성검증_대소문자_정규화() {
        given(attributeRepository.existsBySensorSensorIdAndMetricKey(1L, "co2")).willReturn(true);

        boolean result = attributeService.isValidSensorAttribute(1L, "CO2");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isValidSensorAttribute - 존재하지 않는 메트릭 키는 예외 없이 false")
    void 유효성검증_없는키_false() {
        boolean result = attributeService.isValidSensorAttribute(1L, "unknown_metric");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getAllAttributeBySensorId - 센서 없으면 예외")
    void 목록조회_센서없음() {
        given(sensorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(SensorNotFoundException.class,
                () -> attributeService.getAllAttributeBySensorId(1L, 999L));
    }
}