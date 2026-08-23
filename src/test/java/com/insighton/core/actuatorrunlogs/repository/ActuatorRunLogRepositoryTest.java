package com.insighton.core.actuatorrunlogs.repository;

import com.insighton.core.domain.actuatorrunlogs.entity.ActuatorRunLog;
import com.insighton.core.domain.actuatorrunlogs.entity.CommandType;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuatorrunlogs.repository.ActuatorRunLogRepository;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// actuator-test.sql 픽스처(액추에이터 1: 장소1, 2: 장소3, 3: 장소2)를 기반으로 로그만 직접 save
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/actuator-test.sql")
class ActuatorRunLogRepositoryTest {

    @Autowired
    private ActuatorRunLogRepository actuatorRunLogRepository;

    @Autowired
    private ActuatorRepository actuatorRepository;

    @BeforeEach
    void setUp() {
        Actuator actuator1 = actuatorRepository.findById(1L).orElseThrow(); // 장소 1

        actuatorRunLogRepository.save(ActuatorRunLog.builder()
                .actuator(actuator1).commandType(CommandType.POWER_STATUS).commandValue("ON")
                .executedByType(ExecutedByType.USER).executedByUserId(1L)
                .executedAt(OffsetDateTime.now().minusHours(1)).build());
        actuatorRunLogRepository.save(ActuatorRunLog.builder()
                .actuator(actuator1).commandType(CommandType.SET_TEMPERATURE).commandValue("24")
                .executedByType(ExecutedByType.RULE_ENGINE).executedByUserId(null)
                .executedAt(OffsetDateTime.now()).build());
    }

    @Test
    @DisplayName("findByActuatorActuatorIdOrderByExecutedAtDesc - 최신순 페이지 조회")
    void 액추에이터별_최신순_조회() {
        Page<ActuatorRunLog> result = actuatorRunLogRepository
                .findByActuatorActuatorIdOrderByExecutedAtDesc(1L, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getCommandType()).isEqualTo(CommandType.SET_TEMPERATURE);
    }

    @Test
    @DisplayName("findForReport - 위치 범위 + 기간으로 JOIN FETCH 조회")
    void 리포트용_조회() {
        List<ActuatorRunLog> result = actuatorRunLogRepository.findForReport(
                List.of(1L), OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findForReport - 범위 밖 위치는 제외")
    void 리포트용_조회_범위밖제외() {
        List<ActuatorRunLog> result = actuatorRunLogRepository.findForReport(
                List.of(999L), OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deleteByActuatorActuatorId - 특정 액추에이터 로그만 삭제")
    void 단일액추에이터_로그삭제() {
        actuatorRunLogRepository.deleteByActuatorActuatorId(1L);

        assertThat(actuatorRunLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("deleteAllByActuatorLocationLocationId - 특정 장소 소속 로그만 삭제")
    void 장소기준_로그삭제() {
        actuatorRunLogRepository.deleteAllByActuatorLocationLocationId(1L);

        assertThat(actuatorRunLogRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("deleteAllByActuatorLocationLocationIdIn - 여러 장소 기준 로그 일괄 삭제")
    void 여러장소기준_로그삭제() {
        actuatorRunLogRepository.deleteAllByActuatorLocationLocationIdIn(List.of(1L));

        assertThat(actuatorRunLogRepository.findAll()).isEmpty();
    }
}
