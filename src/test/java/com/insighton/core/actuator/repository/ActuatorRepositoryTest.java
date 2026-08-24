package com.insighton.core.actuator.repository;

import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = "/actuator-test.sql")
class ActuatorRepositoryTest {

    @Autowired
    private ActuatorRepository actuatorRepository;

    @Test
    @DisplayName("findByLocationId_LocationId - 위치 ID로 조회 성공")
    void 위치ID로_조회() {
        List<Actuator> result = actuatorRepository.findByLocationLocationId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSensorName()).isEqualTo("에어컨1");
    }

    @Test
    @DisplayName("findByActuatorType - 타입별 조회 성공")
    void 타입별_조회() {
        List<Actuator> result = actuatorRepository.findByActuatorType(ActuatorType.AIRCON);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("deleteAllByLocationIdLocationIdIn - 특정 위치 스코프 삭제 성공")
    void 위치범위로_스코프삭제() {
        actuatorRepository.deleteAllByLocationLocationIdIn(List.of(1L));

        assertThat(actuatorRepository.findByLocationLocationId(1L)).isEmpty();
        assertThat(actuatorRepository.findByLocationLocationId(2L)).hasSize(1);
    }

    @Test
    @DisplayName("findByLocationLocationIdAndActuatorType - 위치+타입 조합으로 조회 성공")
    void 위치타입조합으로_조회() {
        List<Actuator> result = actuatorRepository.findByLocationLocationIdAndActuatorType(1L, ActuatorType.AIRCON);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSensorName()).isEqualTo("에어컨1");
    }

    @Test
    @DisplayName("findByLocationLocationIdAndActuatorType - 해당 위치에 그 타입이 없으면 빈 리스트")
    void 위치타입조합으로_조회_없음() {
        List<Actuator> result = actuatorRepository.findByLocationLocationIdAndActuatorType(1L, ActuatorType.AIR_PURIFIER);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deleteAllByLocationLocationId - 단일 위치 기준 삭제 성공")
    void 단일위치기준_삭제() {
        actuatorRepository.deleteAllByLocationLocationId(2L);

        assertThat(actuatorRepository.findByLocationLocationId(2L)).isEmpty();
        assertThat(actuatorRepository.findByLocationLocationId(1L)).hasSize(1);
    }
}