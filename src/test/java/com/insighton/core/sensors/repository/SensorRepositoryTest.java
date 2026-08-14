package com.insighton.core.sensors.repository;

import com.insighton.core.domain.sensors.entity.Sensor;
import com.insighton.core.domain.sensors.repository.SensorRepository;
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
@Sql(scripts = "/sensor-test.sql")
class SensorRepositoryTest {

    @Autowired
    private SensorRepository sensorRepository;

    @Test
    @DisplayName("findBySensorEui - EUI로 단건 조회 성공")
    void EUI로_조회() {
        Optional<Sensor> found = sensorRepository.findBySensorEui("EUI-100");

        assertThat(found).isPresent();
        assertThat(found.get().getSensorName()).isEqualTo("센서A");
    }

    @Test
    @DisplayName("findByGroupIdGroupId - 그룹 ID로 조회 성공")
    void 그룹ID로_조회() {
        List<Sensor> result = sensorRepository.findByGroupGroupId(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByLocationLocationName - 위치 이름으로 조회 성공")
    void 위치이름으로_조회() {
        List<Sensor> result = sensorRepository.findByLocationLocationName("4층 개발팀");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSensorEui()).isEqualTo("EUI-104");
    }
}