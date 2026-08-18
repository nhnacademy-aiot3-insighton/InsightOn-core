package com.insighton.core.sensorattributes.repository;

import com.insighton.core.domain.sensorattributes.entity.SensorAttribute;
import com.insighton.core.domain.sensorattributes.repository.SensorAttributeRepository;
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
@Sql(scripts = "/sensor-attribute-test.sql")
class SensorAttributeRepositoryTest {

    @Autowired
    private SensorAttributeRepository attributeRepository;

    @Test
    @DisplayName("findBySensorIdSensorId - 기기 소속 속성 전체 조회 성공")
    void 기기속성_전체조회() {
        List<SensorAttribute> result = attributeRepository.findBySensorSensorId(1L);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findBySensorIdSensorIdAndMetricKey - 복합 조건 단건 조회 성공")
    void 기기와_메트릭키로_단건조회() {
        Optional<SensorAttribute> found =
                attributeRepository.findBySensorSensorIdAndMetricKey(1L, "humidity");

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("deleteBySensorIdSensorId - 기기 삭제 시 속성 일괄 삭제 성공")
    void 기기ID로_일괄삭제() {
        attributeRepository.deleteBySensorSensorId(1L);

        assertThat(attributeRepository.findBySensorSensorId(1L)).isEmpty();
    }

    @Test
    @DisplayName("existsBySensorSensorIdAndMetricKey - 있으면 true, 없으면 false")
    void 존재여부_확인() {
        assertThat(attributeRepository.existsBySensorSensorIdAndMetricKey(1L, "co2")).isTrue();
        assertThat(attributeRepository.existsBySensorSensorIdAndMetricKey(1L, "unknown")).isFalse();
    }

    @Test
    @DisplayName("deleteAllBySensorSensorIdIn - 여러 센서 ID 기준 일괄 삭제 성공")
    void 여러기기ID로_일괄삭제() {
        attributeRepository.deleteAllBySensorSensorIdIn(List.of(1L));

        assertThat(attributeRepository.findBySensorSensorId(1L)).isEmpty();
    }

    @Test
    @DisplayName("deleteBySensorSensorIdAndMetricKey - 특정 속성 하나만 삭제")
    void 단일속성_삭제() {
        attributeRepository.deleteBySensorSensorIdAndMetricKey(1L, "co2");

        List<SensorAttribute> remaining = attributeRepository.findBySensorSensorId(1L);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getMetricKey()).isEqualTo("humidity");
    }

}