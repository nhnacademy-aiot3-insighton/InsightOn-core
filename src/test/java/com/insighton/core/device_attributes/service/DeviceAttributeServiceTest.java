package com.insighton.core.device_attributes.service;

import com.insighton.core.exception.CustomException;
import com.insighton.core.device_attributes.dto.DeviceAttribute;
import com.insighton.core.device_attributes.entity.DeviceAttributeEntity;
import com.insighton.core.device_attributes.repository.DeviceAttributeRepository;
import com.insighton.core.device_attributes.service.impl.DeviceAttributeServiceImpl;
import com.insighton.core.sensors.entity.DeviceEntity;
import com.insighton.core.sensors.entity.DeviceType;
import com.insighton.core.sensors.repository.DeviceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceAttributeServiceTest {

    @Mock private DeviceAttributeRepository attributeRepository;
    @Mock private DeviceRepository deviceRepository;
    @InjectMocks private DeviceAttributeServiceImpl attributeService;

    @Test
    @DisplayName("1. 특정 기기의 전체 속성 목록 조회 성공")
    void getAllAttributeByDeviceId_success() {
        Long deviceId = 1L;
        DeviceEntity device = DeviceEntity.builder().deviceId(deviceId).groupId(1L).deviceType(DeviceType.SENSOR).build();
        DeviceAttributeEntity attr1 = DeviceAttributeEntity.builder().deviceId(device).groupId(1L).metricKey("co2").currentValueStr("800").build();

        when(deviceRepository.existsById(deviceId)).thenReturn(true);
        when(attributeRepository.findByDeviceId_DeviceId(deviceId)).thenReturn(List.of(attr1));

        List<DeviceAttribute> result = attributeService.getAllAttributeByDeviceId(deviceId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).metricKey()).isEqualTo("co2");
    }

    @Test
    @DisplayName("2. 존재하지 않는 기기의 속성 조회 시 예외 발생")
    void getAllAttributeByDeviceId_notFoundDevice() {
        when(deviceRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> attributeService.getAllAttributeByDeviceId(999L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("3. 액추에이터 수치/상태 변경 성공 (Dirty Checking)")
    void updateActuatorValue_success() {
        Long deviceId = 1L;
        String metricKey = "power_status";

        // 성공을 위해 DeviceType.ACTUATOR로 세팅
        DeviceEntity device = DeviceEntity.builder().deviceId(deviceId).groupId(1L).deviceType(DeviceType.ACTUATOR).build();
        DeviceAttributeEntity attribute = DeviceAttributeEntity.builder().deviceId(device).groupId(1L).metricKey(metricKey).currentValueStr("OFF").build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(attributeRepository.findByDeviceId_DeviceIdAndMetricKey(deviceId, metricKey)).thenReturn(Optional.of(attribute));

        attributeService.updateActuatorValue(deviceId, metricKey, "ON");

        assertThat(attribute.getCurrentValueStr()).isEqualTo("ON");
    }

    @Test
    @DisplayName("4. 센서 장치에 대해 제어 명령 시도 시 예외 발생 (400 Bad Request)")
    void updateActuatorValue_rejectSensor() {
        Long deviceId = 1L;

        // SENSOR 기기로 세팅
        DeviceEntity device = DeviceEntity.builder().deviceId(deviceId).groupId(1L).deviceType(DeviceType.SENSOR).build();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> attributeService.updateActuatorValue(deviceId, "power_status", "ON"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("5. 빈 수치값(newValue) 업데이트 요청 시 예외 발생")
    void updateActuatorValue_invalidInputValue() {
        Long deviceId = 1L;
        DeviceEntity device = DeviceEntity.builder().deviceId(deviceId).groupId(1L).deviceType(DeviceType.ACTUATOR).build();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> attributeService.updateActuatorValue(deviceId, "power_status", "  "))
                .isInstanceOf(CustomException.class);
    }
}