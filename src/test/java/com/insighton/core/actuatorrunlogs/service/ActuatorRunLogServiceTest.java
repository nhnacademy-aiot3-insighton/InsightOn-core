package com.insighton.core.actuatorrunlogs.service;

import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogInternalResponse;
import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogResponse;
import com.insighton.core.domain.actuatorrunlogs.entity.ActuatorRunLog;
import com.insighton.core.domain.actuatorrunlogs.entity.CommandType;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuatorrunlogs.event.ActuatorStatusChangedEvent;
import com.insighton.core.domain.actuatorrunlogs.repository.ActuatorRunLogRepository;
import com.insighton.core.domain.actuatorrunlogs.service.impl.ActuatorRunLogServiceImpl;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.location.entity.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActuatorRunLogServiceTest {

    @Mock private ActuatorRunLogRepository actuatorRunLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ActuatorRunLogServiceImpl actuatorRunLogService;

    // 공용 픽스처 - 테스트마다 실제로 타는 getter 조합이 달라서 stub은 전부 lenient 처리
    private Actuator actuator() {
        Group group = mock(Group.class);
        lenient().when(group.getGroupId()).thenReturn(5L);
        Location location = mock(Location.class);
        lenient().when(location.getGroup()).thenReturn(group);
        lenient().when(location.getLocationId()).thenReturn(20L);
        return Actuator.builder().actuatorId(1L).location(location).actuatorType(ActuatorType.AIRCON).build();
    }

    @Test
    @DisplayName("recordRunLogs - 상태값이 비어있으면 아무것도 저장하지 않는다")
    void 기록_빈상태값_스킵() {
        actuatorRunLogService.recordRunLogs(actuator(), Map.of(), ExecutedByType.USER, 1L);

        verify(actuatorRunLogRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("recordRunLogs - POWER_STATUS 명령이면 로그 저장 + InfluxDB 동기화 이벤트 발행")
    void 기록_전원상태_이벤트발행() {
        actuatorRunLogService.recordRunLogs(actuator(), Map.of("power", "ON"), ExecutedByType.USER, 1L);

        verify(actuatorRunLogRepository).saveAll(any());
        verify(eventPublisher).publishEvent(any(ActuatorStatusChangedEvent.class));
    }

    @Test
    @DisplayName("recordRunLogs - POWER_STATUS가 아닌 명령이면 로그는 저장하되 이벤트는 발행 안 함")
    void 기록_비전원명령_이벤트미발행() {
        actuatorRunLogService.recordRunLogs(actuator(), Map.of("temperature", "24.0"), ExecutedByType.USER, 1L);

        verify(actuatorRunLogRepository).saveAll(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("recordRunLogs - 매핑되지 않는 명령키는 저장하지 않고 넘어간다")
    void 기록_알수없는키_스킵() {
        actuatorRunLogService.recordRunLogs(actuator(), Map.of("unknown_key", "value"), ExecutedByType.USER, 1L);

        verify(actuatorRunLogRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("getRunLogsByActuatorId - 레포지토리 결과를 응답 DTO로 매핑해 반환")
    void 실행이력조회_성공() {
        Actuator actuator = actuator();
        ActuatorRunLog log = ActuatorRunLog.builder()
                .runLogId(1L).actuator(actuator).commandType(CommandType.POWER_STATUS)
                .commandValue("ON").executedByType(ExecutedByType.USER).executedByUserId(1L)
                .executedAt(OffsetDateTime.now()).build();
        Pageable pageable = PageRequest.of(0, 20);
        given(actuatorRunLogRepository.findByActuatorActuatorIdOrderByExecutedAtDesc(1L, pageable))
                .willReturn(new PageImpl<>(List.of(log), pageable, 1));

        Page<ActuatorRunLogResponse> result = actuatorRunLogService.getRunLogsByActuatorId(1L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).runLogId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getRunLogsForReport - 레포지토리 결과를 내부 리포트용 DTO로 매핑해 반환")
    void 리포트용조회_성공() {
        Actuator actuator = actuator();
        ActuatorRunLog log = ActuatorRunLog.builder()
                .runLogId(1L).actuator(actuator).commandType(CommandType.POWER_STATUS)
                .commandValue("ON").executedByType(ExecutedByType.USER).executedByUserId(1L)
                .executedAt(OffsetDateTime.now()).build();
        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now();
        given(actuatorRunLogRepository.findForReport(List.of(20L), from, to)).willReturn(List.of(log));

        List<ActuatorRunLogInternalResponse> result = actuatorRunLogService.getRunLogsForReport(List.of(20L), from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).locationId()).isEqualTo(20L);
        assertThat(result.get(0).actuatorId()).isEqualTo(1L);
    }
}
