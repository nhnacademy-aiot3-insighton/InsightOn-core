package com.insighton.core.device_attributes.repositroy;

import com.insighton.core.device_attributes.entity.DeviceAttributeEntity;
import com.insighton.core.device_attributes.repository.DeviceAttributeRepository;
import com.insighton.core.devices.entity.DeviceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DeviceAttributeRepositoryTest {

    @Autowired
    private DeviceAttributeRepository attributeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private DeviceEntity savedDevice;

    @BeforeEach
    void setUp() {
        // 테스트용 부모 DeviceEntity 영속화
        DeviceEntity device = DeviceEntity.builder()
                .deviceName("테스트 센서")
                .deviceEui("EUI-REPO-TEST")
                .gatewaysId(100L)
                .locationsId(1L)
                .createdAt(OffsetDateTime.now())
                .build();
        savedDevice = entityManager.persistAndFlush(device);
    }

    @Test
    @DisplayName("1. 특정 deviceId의 모든 속성 목록 조회")
    void findByDeviceId_DeviceId() {
        // Given
        DeviceAttributeEntity attr1 = DeviceAttributeEntity.builder()
                .deviceId(savedDevice)
                .metricKey("co2")
                .currentValueStr("1000")
                .build();

        DeviceAttributeEntity attr2 = DeviceAttributeEntity.builder()
                .deviceId(savedDevice)
                .metricKey("temperature")
                .currentValueStr("24.5")
                .build();

        entityManager.persist(attr1);
        entityManager.persist(attr2);
        entityManager.flush();

        // When
        List<DeviceAttributeEntity> result = attributeRepository.findByDeviceId_DeviceId(savedDevice.getDeviceId());

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting("metricKey").containsExactlyInAnyOrder("co2", "temperature");
    }

    @Test
    @DisplayName("2. deviceId와 metricKey 조건으로 단일 속성 조회")
    void findByDeviceId_DeviceIdAndMetricKey() {
        // Given
        DeviceAttributeEntity attr = DeviceAttributeEntity.builder()
                .deviceId(savedDevice)
                .metricKey("power_status")
                .currentValueStr("ON")
                .build();
        entityManager.persistAndFlush(attr);

        // When
        Optional<DeviceAttributeEntity> result = attributeRepository
                .findByDeviceId_DeviceIdAndMetricKey(savedDevice.getDeviceId(), "power_status");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getCurrentValueStr()).isEqualTo("ON");
    }

    @Test
    @DisplayName("3. 특정 기기에 해당 metricKey 존재 여부 확인")
    void existsByDeviceId_DeviceIdAndMetricKey() {
        // Given
        DeviceAttributeEntity attr = DeviceAttributeEntity.builder()
                .deviceId(savedDevice)
                .metricKey("humidity")
                .currentValueStr("50")
                .build();
        entityManager.persistAndFlush(attr);

        // When
        boolean exists = attributeRepository.existsByDeviceId_DeviceIdAndMetricKey(savedDevice.getDeviceId(), "humidity");
        boolean notExists = attributeRepository.existsByDeviceId_DeviceIdAndMetricKey(savedDevice.getDeviceId(), "co2");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}