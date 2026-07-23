package com.insighton.core.repository;

import com.insighton.core.entity.DeviceEntity;
import com.insighton.core.entity.DeviceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DeviceRepositoryTest {

    @Autowired
    DeviceRepository deviceRepository;

    @Test
    @DisplayName("1. 게이트웨이ID로 기기 목록 조회")
    void findByGatewaysId() {
        // Given
        deviceRepository.save(
                DeviceEntity.builder().deviceName("센서A").deviceEui("EUI-1").type(DeviceType.SENSOR).gatewaysId(100L).build()
        );
        deviceRepository.save(
                DeviceEntity.builder().deviceName("센서B").deviceEui("EUI-2").type(DeviceType.SENSOR).gatewaysId(200L).build()
        );

        // When
        List<DeviceEntity> result = deviceRepository.findByGatewaysId(100L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceName()).isEqualTo("센서A");
    }

    @Test
    @DisplayName("2. 로케이션ID로 기기 목록 조회")
    void findByLocationsId() {
        // Given
        deviceRepository.save(
                DeviceEntity.builder().deviceName("센서C").deviceEui("EUI-3").type(DeviceType.SENSOR).locationsId(1L).build()
        );

        // When
        List<DeviceEntity> result = deviceRepository.findByLocationsId(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceEui()).isEqualTo("EUI-3");
    }

    @Test
    @DisplayName("3. 이름으로 기기 목록 조회")
    void findByDeviceName() {
        // Given
        deviceRepository.save(
                DeviceEntity.builder().deviceName("센서D").deviceEui("EUI-4").type(DeviceType.SENSOR).build()
        );

        // When
        List<DeviceEntity> result = deviceRepository.findByDeviceName("센서D");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceEui()).isEqualTo("EUI-4");
    }

    @Test
    @DisplayName("4. 타입으로 기기 목록 조회")
    void findByType() {
        // Given
        deviceRepository.save(
                DeviceEntity.builder().deviceName("에어컨A").deviceEui("EUI-5").type(DeviceType.AIRCON).build()
        );
        deviceRepository.save(
                DeviceEntity.builder().deviceName("센서E").deviceEui("EUI-6").type(DeviceType.SENSOR).build()
        );

        // When
        List<DeviceEntity> result = deviceRepository.findByType(DeviceType.AIRCON);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceName()).isEqualTo("에어컨A");
    }

    @Test
    @DisplayName("5. EUI로 기기 단건 조회 성공")
    void findByDeviceEui() {
        // Given
        deviceRepository.save(
                DeviceEntity.builder().deviceName("센서F").deviceEui("EUI-7").type(DeviceType.SENSOR).build()
        );

        // When
        Optional<DeviceEntity> result = deviceRepository.findByDeviceEui("EUI-7");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getDeviceName()).isEqualTo("센서F");
    }

    @Test
    @DisplayName("6. 존재하지 않는 EUI로 조회 시 빈 값 반환")
    void findByDeviceEui_notFound() {
        // When
        Optional<DeviceEntity> result = deviceRepository.findByDeviceEui("NOT-EXIST");

        // Then
        assertThat(result).isEmpty();
    }
}