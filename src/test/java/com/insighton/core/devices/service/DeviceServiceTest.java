package com.insighton.core.devices.service;

import com.insighton.core.exception.CustomException;
import com.insighton.core.devices.dto.DeviceRequest;
import com.insighton.core.devices.dto.DeviceResponse;
import com.insighton.core.devices.entity.DeviceEntity;
import com.insighton.core.devices.repository.DeviceRepository;
import com.insighton.core.devices.service.impl.DeviceServiceImpl; // 💡 구현체 import
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    DeviceRepository deviceRepository;

    @InjectMocks
    DeviceServiceImpl deviceService; // 💡 인터페이스(DeviceService) -> 구현체(DeviceServiceImpl)로 변경

    @Test
    @DisplayName("1. 기기 등록 성공")
    void createDevice() {
        // Given
        DeviceRequest req = new DeviceRequest("센서A", "EUI-1", 100L, 1L);
        DeviceEntity saved = DeviceEntity.builder().deviceId(1L).deviceName("센서A").deviceEui("EUI-1").build();

        // 💡 중복 검사 Pass를 위한 Mocking 추가
        when(deviceRepository.findByDeviceEui("EUI-1")).thenReturn(Optional.empty());
        when(deviceRepository.save(any(DeviceEntity.class))).thenReturn(saved);

        // When
        Long deviceId = deviceService.createDevice(req);

        // Then
        assertThat(deviceId).isEqualTo(1L);
        verify(deviceRepository, times(1)).save(any(DeviceEntity.class));
    }

    @Test
    @DisplayName("2. 단건 기기 조회 성공")
    void getDeviceById() {
        // Given
        DeviceEntity entity = DeviceEntity.builder().deviceId(1L).deviceName("센서B").deviceEui("EUI-2").build();
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(entity));

        // When
        DeviceResponse result = deviceService.getDeviceById(1L);

        // Then
        assertThat(result.deviceName()).isEqualTo("센서B");
    }

    @Test
    @DisplayName("3. 존재하지 않는 기기 조회 시 예외 발생")
    void getDeviceById_notFound() {
        // Given
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> deviceService.getDeviceById(999L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("4. 기기 위치 수정 성공")
    void updateDeviceLocation() {
        // Given
        DeviceEntity entity = DeviceEntity.builder().deviceId(1L).deviceName("센서C").deviceEui("EUI-3").locationsId(1L).build();
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(entity));

        // When
        deviceService.updateDeviceLocation(1L, 99L);

        // Then
        assertThat(entity.getLocationsId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("5. 존재하지 않는 기기 위치 수정 시 예외 발생")
    void updateDeviceLocation_notFound() {
        // Given
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> deviceService.updateDeviceLocation(999L, 1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("6. 패킷 수신 시 마지막 통신 시각 갱신")
    void handlePacketReceived() {
        // Given
        DeviceEntity entity = DeviceEntity.builder().deviceId(1L).deviceName("센서D").deviceEui("EUI-4").build();
        when(deviceRepository.findByDeviceEui("EUI-4")).thenReturn(Optional.of(entity));

        // When
        deviceService.handlePacketReceived("EUI-4");

        // Then
        assertThat(entity.getLastSeenAt()).isNotNull();
    }

    @Test
    @DisplayName("7. 존재하지 않는 EUI로 패킷 수신 시 아무 일도 일어나지 않음")
    void handlePacketReceived_notFound() {
        // Given
        when(deviceRepository.findByDeviceEui("NOT-EXIST")).thenReturn(Optional.empty());

        // When & Then
        deviceService.handlePacketReceived("NOT-EXIST");
        verify(deviceRepository, never()).save(any());
    }

    @Test
    @DisplayName("8. 기기 삭제 성공")
    void deleteDevice() {
        // Given
        DeviceEntity entity = DeviceEntity.builder().deviceId(1L).deviceName("센서E").deviceEui("EUI-5").build();
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(entity));

        // When
        deviceService.deleteDevice(1L);

        // Then
        verify(deviceRepository, times(1)).delete(entity);
    }

    @Test
    @DisplayName("9. 존재하지 않는 기기 삭제 시 예외 발생")
    void deleteDevice_notFound() {
        // Given
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> deviceService.deleteDevice(999L))
                .isInstanceOf(CustomException.class);
        verify(deviceRepository, never()).delete(any());
    }

    @Test
    @DisplayName("10. 전체 삭제 성공")
    void deleteAll() {
        // When
        deviceService.deleteAll();

        // Then
        verify(deviceRepository, times(1)).deleteAll();
    }

    @Test
    @DisplayName("11. ID로 검색 성공")
    void searchDevices_byId() {
        // Given
        DeviceEntity entity = DeviceEntity.builder().deviceId(1L).deviceName("센서F").deviceEui("EUI-6").build();
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(entity));

        // When
        List<DeviceResponse> result = deviceService.searchDevices(1L, null, null, null, null);

        // Then
        assertThat(result).hasSize(1);
        verify(deviceRepository, never()).findAll();
    }

    @Test
    @DisplayName("12. 아무 조건도 없으면 전체 조회")
    void searchDevices_noCondition() {
        // Given
        DeviceEntity entity = DeviceEntity.builder().deviceId(1L).deviceName("센서G").deviceEui("EUI-7").build();
        when(deviceRepository.findAll()).thenReturn(List.of(entity));

        // When
        List<DeviceResponse> result = deviceService.searchDevices(null, null, null, null, null);

        // Then
        assertThat(result).hasSize(1);
        verify(deviceRepository, times(1)).findAll();
    }
}