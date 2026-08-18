package com.insighton.core.sensorattributes.repository;

import com.insighton.core.domain.sensorattributes.entity.MetricDefinition;
import com.insighton.core.domain.sensorattributes.repository.MetricDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// MetricDefinition은 FK가 없는 전역 카탈로그라 SQL 픽스처 없이 직접 save해서 구성
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MetricDefinitionRepositoryTest {

    @Autowired
    private MetricDefinitionRepository metricDefinitionRepository;

    @BeforeEach
    void setUp() {
        metricDefinitionRepository.save(MetricDefinition.builder()
                .metricKey("co2").metricName("이산화탄소").unit("ppm").build());
    }

    @Test
    @DisplayName("findByMetricKeyIgnoreCase - 대소문자 달라도 조회 성공")
    void 대소문자무시_조회_성공() {
        Optional<MetricDefinition> found = metricDefinitionRepository.findByMetricKeyIgnoreCase("CO2");

        assertThat(found).isPresent();
        assertThat(found.get().getMetricName()).isEqualTo("이산화탄소");
    }

    @Test
    @DisplayName("findByMetricKeyIgnoreCase - 등록되지 않은 키면 빈 Optional")
    void 없는키_조회시_빈값() {
        Optional<MetricDefinition> found = metricDefinitionRepository.findByMetricKeyIgnoreCase("unknown");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("save - metricKey가 PK라 같은 키로 다시 save하면 덮어씀")
    void 저장_동일키_덮어씀() {
        metricDefinitionRepository.save(MetricDefinition.builder()
                .metricKey("co2").metricName("CO2 농도").unit("ppm").build());

        assertThat(metricDefinitionRepository.findAll()).hasSize(1);
        assertThat(metricDefinitionRepository.findById("co2").orElseThrow().getMetricName()).isEqualTo("CO2 농도");
    }
}
