
package com.insighton.core.device_attributes.service;

import com.insighton.core.exception.CustomException;
import com.insighton.core.device_attributes.dto.DeviceAttribute;
import com.insighton.core.device_attributes.entity.DeviceAttributeEntity;
import com.insighton.core.device_attributes.repository.DeviceAttributeRepository;
import com.insighton.core.device_attributes.service.impl.DeviceAttributeServiceImpl;
import com.insighton.core.devices.entity.DeviceEntity;
import com.insighton.core.devices.repository.DeviceRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceAttributeServiceTest {

    @Mock
    private DeviceAttributeRepository attributeRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceAttributeServiceImpl attributeService;

    @Test
    @DisplayName("1. 특정 기기의 전체 속성 목록 조회 성공")
    void getAllAttributeByDeviceId_success() {
        // Given
        Long deviceId = 1L;
        DeviceEntity device = DeviceEntity.builder().deviceId(deviceId).build();
        DeviceAttributeEntity attr1 = DeviceAttributeEntity.builder()
                .deviceId(device).metricKey("co2").currentValueStr("800").build();
        DeviceAttributeEntity attr2 = DeviceAttributeEntity.builder()
                .deviceId(device).metricKey("temperature").currentValueStr("23.0").build();

        when(deviceRepository.existsById(deviceId)).thenReturn(true);
        when(attributeRepository.findByDeviceId_DeviceId(deviceId)).thenReturn(List.of(attr1, attr2));

        // When
        List<DeviceAttribute> result = attributeService.getAllAttributeByDeviceId(deviceId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).metricKey()).isEqualTo("co2");
        assertThat(result.get(0).displayName()).isEqualTo("이산화탄소");
        assertThat(result.get(0).unit()).isEqualTo("ppm");
    }

    @Test
    @DisplayName("2. 존재하지 않는 기기의 속성 조회 시 예외 발생")
    void getAllAttributeByDeviceId_notFoundDevice() {
        // Given
        when(deviceRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> attributeService.getAllAttributeByDeviceId(999L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("3. 액추에이터 수치/상태 변경 성공 (Dirty Checking)")
    void updateActuatorValue_success() {
        // Given
        Long deviceId = 1L;
        String metricKey = "power_status";
        String newValue = "ON";

        DeviceEntity device = DeviceEntity.builder().deviceId(deviceId).build();
        DeviceAttributeEntity attribute = DeviceAttributeEntity.builder()
                .deviceId(device).metricKey(metricKey).currentValueStr("OFF").build();

        when(deviceRepository.existsById(deviceId)).thenReturn(true);
        when(attributeRepository.findByDeviceId_DeviceIdAndMetricKey(deviceId, metricKey))
                .thenReturn(Optional.of(attribute));

        // When
        attributeService.updateActuatorValue(deviceId, metricKey, newValue);

        // Then
        assertThat(attribute.getCurrentValueStr()).isEqualTo("ON");
    }

    @Test
    @DisplayName("4. 빈 수치값(newValue) 업데이트 요청 시 예외 발생")
    void updateActuatorValue_invalidInputValue() {
        // When & Then
        assertThatThrownBy(() -> attributeService.updateActuatorValue(1L, "power_status", "  "))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("5. 잘못된 메트릭 키로 상태 변경 요청 시 예외 발생")
    void updateActuatorValue_invalidMetricKey() {
        // Given
        when(deviceRepository.existsById(1L)).thenReturn(true);

        // When & Then (MetricDefinition에 없는 키)
        assertThatThrownBy(() -> attributeService.updateActuatorValue(1L, "invalid_key", "ON"))
                .isInstanceOf(CustomException.class);
    }
}