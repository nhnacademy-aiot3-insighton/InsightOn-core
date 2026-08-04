package com.insighton.core.sensors.repository;

import com.insighton.core.sensors.entity.Device;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/device-test.sql")
class DeviceRepositoryTest {

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    @DisplayName("findByDeviceEui - EUI로 단건 조회 성공")
    void EUI로_조회() {
        Optional<Device> found = deviceRepository.findByDeviceEui("EUI-100");

        assertThat(found).isPresent();
        assertThat(found.get().getDeviceName()).isEqualTo("센서A");
    }

    @Test
    @DisplayName("findByGroupIdGroupId - 그룹 ID로 조회 성공")
    void 그룹ID로_조회() {
        List<Device> result = deviceRepository.findByGroupIdGroupId(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByLocationsIdLocationId - 위치 ID로 조회 성공")
    void 위치ID로_조회() {
        List<Device> result = deviceRepository.findByLocationsIdLocationId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceEui()).isEqualTo("EUI-104");
    }
}