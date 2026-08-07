package com.insighton.core.actuator.service;

import com.insighton.core.actuator_run_logs.entity.ExecutedByType;
import com.insighton.core.actuator_run_logs.repository.ActuatorRunLogRepository;
import com.insighton.core.actuator_run_logs.service.ActuatorRunLogService;
import com.insighton.core.actuators.dto.ActuatorRequest;
import com.insighton.core.actuators.entity.ActuatorType;
import com.insighton.core.actuators.entity.Actuator;
import com.insighton.core.actuators.exception.ActuatorNotFoundException;
import com.insighton.core.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.actuators.repository.ActuatorRepository;
import com.insighton.core.actuators.service.impl.ActuatorServiceImpl;
import com.insighton.core.groupmember.entity.GroupMember;
import com.insighton.core.groupmember.entity.GroupMember.GroupRole;
import com.insighton.core.groupmember.service.GroupMemberService;
import com.insighton.core.location.entity.Location;
import com.insighton.core.location.exception.LocationNotFoundException;
import com.insighton.core.location.repository.LocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActuatorServiceTest {

    @Mock private ActuatorRepository actuatorRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private GroupMemberService groupMemberService;
    @Mock private ActuatorRunLogService actuatorRunLogService; // 없으면 상태변경 시 NPE
    @Mock private ActuatorRunLogRepository actuatorRunLogRepository; // 없으면 삭제 시 NPE

    @InjectMocks
    private ActuatorServiceImpl actuatorsService;

    private GroupMember manager() {
        return GroupMember.builder().userId(1L).groupRole(GroupRole.MANAGER).build();
    }

    private GroupMember plainMember() {
        return GroupMember.builder().userId(1L).groupRole(GroupRole.MEMBER).build();
    }

    @Test
    @DisplayName("createActuator - locationId가 groupsId 소속이 아니면 LocationNotFoundException")
    void 생성_다른그룹location_거부() {
        given(groupMemberService.validateGroupMembers(10L, 1L)).willReturn(manager());
        given(locationRepository.findByLocationIdAndGroupGroupId(99L, 10L)).willReturn(Optional.empty());

        ActuatorRequest request = new ActuatorRequest(99L, "에어컨", ActuatorType.AIRCON, Map.of("power", "OFF"));

        assertThrows(LocationNotFoundException.class,
                () -> actuatorsService.createActuator(1L, 10L, request));

        verify(actuatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("createActuator - MEMBER 권한이면 거부")
    void 생성_권한없음() {
        given(groupMemberService.validateGroupMembers(10L, 1L)).willReturn(plainMember());

        ActuatorRequest request = new ActuatorRequest(1L, "에어컨", ActuatorType.AIRCON, Map.of("power", "OFF"));

        assertThrows(com.insighton.core.groups.exception.NoPermissionException.class,
                () -> actuatorsService.createActuator(1L, 10L, request));
    }

    @Test
    @DisplayName("getActuatorById - 다른 그룹 소속 액추에이터면 존재하지 않는 것처럼 404")
    void 조회_다른그룹_소속아니면_404() {
        Location location = mock(Location.class);
        given(location.getLocationId()).willReturn(50L);
        Actuator entity = Actuator.builder().actuatorId(1L).location(location).build();

        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
        given(groupMemberService.validateGroupMembers(10L, 1L)).willReturn(manager());
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 10L)).willReturn(Optional.empty());

        assertThrows(ActuatorNotFoundException.class,
                () -> actuatorsService.getActuatorById(1L, 10L, 1L));
    }

    @Test
    @DisplayName("updateActuatorState - USER가 아닌 시스템 요청이면 권한/소유권 체크를 건너뛴다")
    void 상태변경_시스템요청_권한체크생략() {
        Location location = mock(Location.class);
        Actuator entity = Actuator.builder().actuatorId(1L).location(location).build();
        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));

        actuatorsService.updateActuatorState(null, null, 1L, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE);

        verify(groupMemberService, never()).validateGroupMembers(any(), any());
        verify(locationRepository, never()).findByLocationIdAndGroupGroupId(any(), any());
        verify(actuatorRunLogService).recordRunLogs(entity, Map.of("power", "ON"), ExecutedByType.RULE_ENGINE, null);
    }

    @Test
    @DisplayName("updateActuatorState - 빈 상태값이면 InvalidActuatorValueException")
    void 상태변경_빈값_거부() {
        given(groupMemberService.validateGroupMembers(10L, 1L)).willReturn(manager());

        assertThrows(InvalidActuatorValueException.class,
                () -> actuatorsService.updateActuatorState(1L, 10L, 1L, Map.of(), ExecutedByType.USER));
    }

    @Test
    @DisplayName("deleteActuatorById - 소유권 체크 통과 후 정상 삭제, 실행로그 먼저 삭제")
    void 삭제_성공() {
        Location location = mock(Location.class);
        given(location.getLocationId()).willReturn(50L);
        Actuator entity = Actuator.builder().actuatorId(1L).location(location).build();

        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
        given(groupMemberService.validateGroupMembers(10L, 1L)).willReturn(manager());
        given(locationRepository.findByLocationIdAndGroupGroupId(50L, 10L)).willReturn(Optional.of(location));

        actuatorsService.deleteActuatorById(1L, 10L, 1L);

        verify(actuatorRunLogRepository).deleteByActuatorActuatorId(1L);
        verify(actuatorRepository).delete(entity);
    }

    @Test
    @DisplayName("deleteAll - groupsId 소속 location 범위로 실행로그/액추에이터 둘 다 스코프 삭제")
    void 전체삭제_그룹스코프() {
        given(groupMemberService.validateGroupMembers(10L, 1L)).willReturn(manager());
        given(locationRepository.findAllByGroupGroupId(10L)).willReturn(List.of());

        actuatorsService.deleteAll(1L, 10L);

        verify(actuatorRunLogRepository).deleteAllByActuatorLocationLocationIdIn(List.of());
        verify(actuatorRepository).deleteAllByLocationLocationIdIn(List.of());
    }
}