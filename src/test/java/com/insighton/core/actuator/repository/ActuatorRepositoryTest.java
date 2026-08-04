package com.insighton.core.actuator.repository;

import com.insighton.core.actuators.entity.ActuatorType;
import com.insighton.core.actuators.entity.Actuator;
import com.insighton.core.actuators.repository.ActuatorRepository;
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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/actuator-test.sql")
class ActuatorRepositoryTest {

    @Autowired
    private ActuatorRepository actuatorRepository;

    @Test
    @DisplayName("findByLocationId_LocationId - 위치 ID로 조회 성공")
    void 위치ID로_조회() {
        List<Actuator> result = actuatorRepository.findByLocationId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceName()).isEqualTo("에어컨1");
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
        actuatorRepository.deleteAllByLocationIdList(List.of(1L));

        assertThat(actuatorRepository.findByLocationId(1L)).isEmpty();
        assertThat(actuatorRepository.findByLocationId(2L)).hasSize(1);
    }
}