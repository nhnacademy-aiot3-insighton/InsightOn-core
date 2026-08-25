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

    @Test
    @DisplayName("findByGroupGroupIdAndLocationIsNull - 장소 미배정 센서만 조회")
    void 장소미배정_센서만_조회() {
        List<Sensor> result = sensorRepository.findByGroupGroupIdAndLocationIsNull(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSensorEui()).isEqualTo("EUI-100");
    }

    @Test
    @DisplayName("findBySensorName - 이름으로 조회 성공")
    void 이름으로_조회() {
        List<Sensor> result = sensorRepository.findBySensorName("센서A");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSensorEui()).isEqualTo("EUI-100");
    }

    @Test
    @DisplayName("findByGatewayGatewayId - 게이트웨이 ID로 조회 성공")
    void 게이트웨이ID로_조회() {
        List<Sensor> result = sensorRepository.findByGatewayGatewayId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSensorEui()).isEqualTo("EUI-100");
    }

    @Test
    @DisplayName("findByGroupGroupIdAndLocationLocationId - 그룹+장소 조합으로 조회 성공")
    void 그룹장소조합으로_조회() {
        List<Sensor> result = sensorRepository.findByGroupGroupIdAndLocationLocationId(1L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSensorEui()).isEqualTo("EUI-104");
    }

    @Test
    @DisplayName("deleteAllByLocationLocationId - 해당 장소 소속 센서만 삭제")
    void 장소기준_일괄삭제() {
        sensorRepository.deleteAllByLocationLocationId(1L);

        assertThat(sensorRepository.findAll()).hasSize(1);
        assertThat(sensorRepository.findBySensorEui("EUI-100")).isPresent();
    }

    @Test
    @DisplayName("deleteAllByGroupGroupId - 해당 그룹 소속 센서 전부 삭제")
    void 그룹기준_일괄삭제() {
        sensorRepository.deleteAllByGroupGroupId(1L);

        assertThat(sensorRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("search - 조건 없으면 groupId 소속 전체 반환")
    void 검색_조건없음_그룹전체() {
        List<Sensor> result = sensorRepository.search(1L, null, null, null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("search - id로 단건 좁히기")
    void 검색_id조건() {
        List<Sensor> result = sensorRepository.search(1L, 1L, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSensorEui()).isEqualTo("EUI-100");
    }

    @Test
    @DisplayName("search - eui로 단건 좁히기")
    void 검색_eui조건() {
        List<Sensor> result = sensorRepository.search(1L, null, "EUI-104", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSensorId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("search - locationId로 단건 좁히기")
    void 검색_locationId조건() {
        List<Sensor> result = sensorRepository.search(1L, null, null, 1L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSensorEui()).isEqualTo("EUI-104");
    }

    @Test
    @DisplayName("search - sensorName으로 단건 좁히기")
    void 검색_sensorName조건() {
        List<Sensor> result = sensorRepository.search(1L, null, null, null, "센서A");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSensorEui()).isEqualTo("EUI-100");
    }

    @Test
    @DisplayName("search - 다른 그룹의 groupId를 넘기면 그 그룹 소속 센서는 안 나옴")
    void 검색_다른그룹은_제외() {
        List<Sensor> result = sensorRepository.search(2L, null, null, null, null);

        assertThat(result).isEmpty();
    }
}