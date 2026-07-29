package com.insighton.core.sensors.service;

import com.insighton.core.exception.CustomException;
import com.insighton.core.sensors.dto.DeviceResponse;
import com.insighton.core.sensors.entity.DeviceCacheEntry;
import com.insighton.core.sensors.entity.DeviceEntity;
import com.insighton.core.sensors.entity.DeviceType;
import com.insighton.core.sensors.repository.DeviceRepository;
import com.insighton.core.device_attributes.repository.DeviceAttributeRepository;
import com.insighton.core.sensors.service.impl.DeviceServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock DeviceRepository deviceRepository;
    @Mock DeviceAttributeRepository deviceAttributeRepository;
    @Mock DeviceLookupCacheService deviceLookupCacheService; // 새로 추가된 의존성 Mocking

    @InjectMocks
    DeviceServiceImpl deviceService;

    @Test
    @DisplayName("1. 기기 자동 등록(autoProvision) 성공")
    void autoProvision() {
        // Given
        Long groupId = 10L;
        DeviceEntity saved = DeviceEntity.builder().deviceId(1L).groupId(groupId).deviceName("SENSOR_A").deviceEui("EUI-1").build();

        when(deviceRepository.save(any(DeviceEntity.class))).thenReturn(saved);

        // When
        DeviceCacheEntry cacheEntry = deviceService.autoProvision(groupId, 100L, "EUI-1", "Sensor_a", Set.of("co2"));

        // Then
        assertThat(cacheEntry.deviceEui()).isEqualTo("EUI-1");
        verify(deviceRepository, times(1)).save(any(DeviceEntity.class));
        verify(deviceAttributeRepository, times(1)).saveAll(anyList());
        verify(deviceLookupCacheService, times(1)).populate(any(DeviceCacheEntry.class));
    }

    @Test
    @DisplayName("2. 단건 기기 조회 성공")
    void getDeviceById() {
        DeviceEntity entity = DeviceEntity.builder().deviceId(1L).groupId(1L).deviceType(DeviceType.SENSOR).deviceName("센서B").deviceEui("EUI-2").build();
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(entity));

        DeviceResponse result = deviceService.getDeviceById(1L);
        assertThat(result.deviceName()).isEqualTo("센서B");
    }

    @Test
    @DisplayName("4. 기기 위치 수정 성공")
    void updateDeviceLocation() {
        DeviceEntity entity = DeviceEntity.builder().deviceId(1L).groupId(1L).deviceType(DeviceType.SENSOR).deviceName("센서C").deviceEui("EUI-3").locationsId(1L).build();
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(entity));

        deviceService.updateDeviceLocation(1L, 99L);
        assertThat(entity.getLocationsId()).isEqualTo(99L);
    }
}