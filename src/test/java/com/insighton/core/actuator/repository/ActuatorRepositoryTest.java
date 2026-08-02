package com.insighton.core.actuator.repository;

import com.insighton.core.actuators.entity.ActuatorType;
import com.insighton.core.actuators.entity.ActuatorsEntity;
import com.insighton.core.actuators.repository.ActuatorsRepository;
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
    private ActuatorsRepository actuatorsRepository;

    @Test
    @DisplayName("findByLocationId_LocationId - 위치 ID로 조회 성공")
    void 위치ID로_조회() {
        List<ActuatorsEntity> result = actuatorsRepository.findByLocationId_LocationId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceName()).isEqualTo("에어컨1");
    }

    @Test
    @DisplayName("findByActuatorType - 타입별 조회 성공")
    void 타입별_조회() {
        List<ActuatorsEntity> result = actuatorsRepository.findByActuatorType(ActuatorType.AIRCON);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("deleteAllByLocationId_LocationIdIn - 특정 위치 스코프 삭제 성공")
    void 위치범위로_스코프삭제() {
        actuatorsRepository.deleteAllByLocationId_LocationIdIn(List.of(1L));

        assertThat(actuatorsRepository.findByLocationId_LocationId(1L)).isEmpty();
        assertThat(actuatorsRepository.findByLocationId_LocationId(2L)).hasSize(1);
    }
}