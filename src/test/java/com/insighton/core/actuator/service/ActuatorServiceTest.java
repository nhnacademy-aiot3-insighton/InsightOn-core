package com.insighton.core.actuator.service;

import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuatorrunlogs.repository.ActuatorRunLogRepository;
import com.insighton.core.domain.actuatorrunlogs.service.ActuatorRunLogService;
import com.insighton.core.domain.actuators.dto.ActuatorRequest;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.entity.Actuator;
import com.insighton.core.domain.actuators.exception.ActuatorNotFoundException;
import com.insighton.core.domain.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.domain.actuators.repository.ActuatorRepository;
import com.insighton.core.domain.actuators.service.impl.ActuatorServiceImpl;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import com.insighton.core.domain.location.repository.LocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// 권한 체크(그룹 멤버십/매니저 검증)는 usecase.actuator 패키지로 이동해서, 이 클래스는
// 순수 영속성 로직(소유권 교차검증 포함)만 검증함 - 권한 관련 시나리오는 각 UseCase 테스트에서 다뤄야 함
@ExtendWith(MockitoExtension.class)
class ActuatorServiceTest {

    @Mock private ActuatorRepository actuatorRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private ActuatorRunLogService actuatorRunLogService; // 없으면 상태변경 시 NPE
    @Mock private ActuatorRunLogRepository actuatorRunLogRepository; // 없으면 삭제 시 NPE

    @InjectMocks
    private ActuatorServiceImpl actuatorsService;

    @Test
    @DisplayName("createActuator - locationName이 groupsId 소속이 아니면 LocationNotFoundException")
    void 생성_다른그룹location_거부() {
        given(locationRepository.findByGroupGroupIdAndLocationName(10L, "없는장소")).willReturn(Optional.empty());

        ActuatorRequest request = new ActuatorRequest("없는장소", "에어컨", ActuatorType.AIRCON, Map.of("power", "OFF"));

        assertThrows(LocationNotFoundException.class,
                () -> actuatorsService.createActuator(10L, request));

        verify(actuatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("getActuatorById - 다른 그룹 소속 액추에이터면 존재하지 않는 것처럼 404")
    void 조회_다른그룹_소속아니면_404() {
        Location location = mock(Location.class);
        given(location.getLocationId()).willReturn(50L);
        Actuator entity = Actuator.builder().actuatorId(1L).location(location).build();

        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 10L)).willReturn(Optional.empty());

        assertThrows(ActuatorNotFoundException.class,
                () -> actuatorsService.getActuatorById(10L, 1L));
    }

    @Test
    @DisplayName("getActuatorsByLocationId - 위치 소속의 액추에이터 목록 반환")
    void 위치별조회_성공() {
        Location location = mock(Location.class);
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 10L)).willReturn(Optional.of(location));

        Actuator entity = Actuator.builder().actuatorId(1L).location(location).build();
        given(actuatorRepository.findByLocationLocationId(50L)).willReturn(List.of(entity));

        List<ActuatorResponse> result = actuatorsService.getActuatorsByLocationId(10L, 50L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).actuatorId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getActuatorsByLocationId - 위치가 다른 그룹 소속이면 LocationNotFoundException")
    void 위치별조회_다른그룹_거부() {
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 10L)).willReturn(Optional.empty());

        assertThrows(LocationNotFoundException.class,
                () -> actuatorsService.getActuatorsByLocationId(10L, 50L));

        verify(actuatorRepository, never()).findByLocationLocationId(any());
    }

    @Test
    @DisplayName("updateActuatorState - USER가 아닌 시스템 요청이면 소유권 체크를 건너뛴다")
    void 상태변경_시스템요청_소유권체크생략() {
        Location location = mock(Location.class);
        Actuator entity = Actuator.builder().actuatorId(1L).location(location).build();
        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));

        actuatorsService.updateActuatorState(null, 1L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);

        verify(locationRepository, never()).findByLocationIdAndGroupGroupId(any(), any());
        verify(actuatorRunLogService).recordRunLogs(entity, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);
    }

    @Test
    @DisplayName("updateActuatorState - 빈 상태값이면 InvalidActuatorValueException")
    void 상태변경_빈값_거부() {
        assertThrows(InvalidActuatorValueException.class,
                () -> actuatorsService.updateActuatorState(10L, 1L, Map.of(), ExecutedByType.USER, 1L));
    }

    @Test
    @DisplayName("deleteActuatorById - 소유권 체크 통과 후 정상 삭제, 실행로그 먼저 삭제")
    void 삭제_성공() {
        Location location = mock(Location.class);
        given(location.getLocationId()).willReturn(50L);
        Actuator entity = Actuator.builder().actuatorId(1L).location(location).build();

        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 10L)).willReturn(Optional.of(location));

        actuatorsService.deleteActuatorById(10L, 1L);

        verify(actuatorRunLogRepository).deleteByActuatorActuatorId(1L);
        verify(actuatorRepository).delete(entity);
    }

    @Test
    @DisplayName("deleteAll - groupsId 소속 location 범위로 실행로그/액추에이터 둘 다 스코프 삭제")
    void 전체삭제_그룹스코프() {
        given(locationRepository.findAllByGroupGroupId(10L)).willReturn(List.of());

        actuatorsService.deleteAll(10L);

        verify(actuatorRunLogRepository).deleteAllByActuatorLocationLocationIdIn(List.of());
        verify(actuatorRepository).deleteAllByLocationLocationIdIn(List.of());
    }

    @Test
    @DisplayName("createActuator - 정상 생성, 생성된 ID 반환")
    void 생성_성공() {
        Location location = mock(Location.class);
        given(locationRepository.findByGroupGroupIdAndLocationName(10L, "거실")).willReturn(Optional.of(location));
        given(actuatorRepository.save(any(Actuator.class))).willReturn(Actuator.builder().actuatorId(100L).build());

        ActuatorRequest request = new ActuatorRequest("거실", "에어컨", ActuatorType.AIRCON, Map.of("power", "OFF"));
        Long result = actuatorsService.createActuator(10L, request);

        assertThat(result).isEqualTo(100L);
    }

    @Test
    @DisplayName("getActuatorById - 액추에이터 자체가 없으면 ActuatorNotFoundException")
    void 조회_없는액추에이터() {
        given(actuatorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(ActuatorNotFoundException.class,
                () -> actuatorsService.getActuatorById(10L, 999L));
    }

    @Test
    @DisplayName("getActuatorById - 정상 조회")
    void 조회_성공() {
        Location location = mock(Location.class);
        given(location.getLocationId()).willReturn(50L);
        Actuator entity = Actuator.builder().actuatorId(1L).location(location)
                .sensorName("에어컨").actuatorType(ActuatorType.AIRCON).currentState(Map.of("power", "ON")).build();

        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 10L)).willReturn(Optional.of(location));

        ActuatorResponse result = actuatorsService.getActuatorById(10L, 1L);

        assertThat(result.actuatorId()).isEqualTo(1L);
        assertThat(result.sensorName()).isEqualTo("에어컨");
    }

    @Test
    @DisplayName("updateActuatorState - 없는 액추에이터면 ActuatorNotFoundException")
    void 상태변경_없는액추에이터() {
        given(actuatorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(ActuatorNotFoundException.class,
                () -> actuatorsService.updateActuatorState(10L, 999L, Map.of("power", "ON"), ExecutedByType.USER, 1L));
    }

    @Test
    @DisplayName("updateActuatorState - USER 요청이면 실제로 소유권 체크를 수행하고 통과 시 반영")
    void 상태변경_유저요청_소유권체크통과() {
        Location location = mock(Location.class);
        given(location.getLocationId()).willReturn(50L);
        Actuator entity = Actuator.builder().actuatorId(1L).location(location).build();
        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 10L)).willReturn(Optional.of(location));

        actuatorsService.updateActuatorState(10L, 1L, Map.of("power", "ON"), ExecutedByType.USER, 1L);

        verify(locationRepository).findByLocationIdAndGroupGroupId(50L, 10L);
        verify(actuatorRunLogService).recordRunLogs(entity, Map.of("power", "ON"), ExecutedByType.USER, 1L);
    }

    @Test
    @DisplayName("updateActuatorState - USER 요청인데 다른 그룹 소속이면 ActuatorNotFoundException, 로그 기록 안 함")
    void 상태변경_유저요청_다른그룹_거부() {
        Location location = mock(Location.class);
        given(location.getLocationId()).willReturn(50L);
        Actuator entity = Actuator.builder().actuatorId(1L).location(location).build();
        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 10L)).willReturn(Optional.empty());

        assertThrows(ActuatorNotFoundException.class,
                () -> actuatorsService.updateActuatorState(10L, 1L, Map.of("power", "ON"), ExecutedByType.USER, 1L));

        verify(actuatorRunLogService, never()).recordRunLogs(any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateActuatorName - 정상 수정")
    void 이름수정_성공() {
        Location location = mock(Location.class);
        given(location.getLocationId()).willReturn(50L);
        Actuator entity = Actuator.builder().actuatorId(1L).location(location).sensorName("기존이름").build();

        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 10L)).willReturn(Optional.of(location));

        actuatorsService.updateActuatorName(10L, 1L, "새이름");

        assertThat(entity.getSensorName()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("updateActuatorName - 빈 값이면 InvalidActuatorValueException, 조회도 안 함")
    void 이름수정_빈값_거부() {
        assertThrows(InvalidActuatorValueException.class,
                () -> actuatorsService.updateActuatorName(10L, 1L, "   "));

        verify(actuatorRepository, never()).findById(any());
    }

    @Test
    @DisplayName("updateActuatorName - 없는 액추에이터면 ActuatorNotFoundException")
    void 이름수정_없는액추에이터() {
        given(actuatorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(ActuatorNotFoundException.class,
                () -> actuatorsService.updateActuatorName(10L, 999L, "새이름"));
    }

    @Test
    @DisplayName("updateActuatorName - 다른 그룹 소속이면 ActuatorNotFoundException, 이름 변경 안 됨")
    void 이름수정_다른그룹_거부() {
        Location location = mock(Location.class);
        given(location.getLocationId()).willReturn(50L);
        Actuator entity = Actuator.builder().actuatorId(1L).location(location).sensorName("기존이름").build();

        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 10L)).willReturn(Optional.empty());

        assertThrows(ActuatorNotFoundException.class,
                () -> actuatorsService.updateActuatorName(10L, 1L, "새이름"));

        assertThat(entity.getSensorName()).isEqualTo("기존이름");
    }

    @Test
    @DisplayName("deleteActuatorById - 없는 액추에이터면 ActuatorNotFoundException, 삭제 안 함")
    void 삭제_없는액추에이터() {
        given(actuatorRepository.findById(999L)).willReturn(Optional.empty());

        assertThrows(ActuatorNotFoundException.class,
                () -> actuatorsService.deleteActuatorById(10L, 999L));

        verify(actuatorRunLogRepository, never()).deleteByActuatorActuatorId(any());
    }

    @Test
    @DisplayName("deleteActuatorById - 다른 그룹 소속이면 ActuatorNotFoundException, 삭제 안 함")
    void 삭제_다른그룹_거부() {
        Location location = mock(Location.class);
        given(location.getLocationId()).willReturn(50L);
        Actuator entity = Actuator.builder().actuatorId(1L).location(location).build();

        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 10L)).willReturn(Optional.empty());

        assertThrows(ActuatorNotFoundException.class,
                () -> actuatorsService.deleteActuatorById(10L, 1L));

        verify(actuatorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteAllByLocationId - 해당 위치 소속 실행로그/액추에이터 스코프 삭제")
    void 장소기준_전체삭제() {
        actuatorsService.deleteAllByLocationId(50L);

        verify(actuatorRunLogRepository).deleteAllByActuatorLocationLocationId(50L);
        verify(actuatorRepository).deleteAllByLocationLocationId(50L);
    }
}
