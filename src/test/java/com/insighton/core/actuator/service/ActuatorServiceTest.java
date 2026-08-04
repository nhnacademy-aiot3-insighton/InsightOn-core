//package com.insighton.core.actuator.service;
//
//import com.insighton.core.actuators.dto.ActuatorRequest;
//import com.insighton.core.actuators.entity.ActuatorType;
//import com.insighton.core.actuators.entity.Actuator;
//import com.insighton.core.actuators.exception.ActuatorNotFoundException;
//import com.insighton.core.actuators.exception.InvalidActuatorValueException;
//import com.insighton.core.actuators.repository.ActuatorRepository;
//import com.insighton.core.actuators.service.impl.ActuatorServiceImpl;
//import com.insighton.core.groupmember.entity.GroupMembers;
//import com.insighton.core.groupmember.entity.GroupMembers.GroupRole;
//import com.insighton.core.groupmember.service.GroupMembersService;
//import com.insighton.core.location.entity.Locations;
//import com.insighton.core.location.exception.LocationNotFoundException;
//import com.insighton.core.location.repository.LocationsRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Map;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class ActuatorServiceTest {
//
//    @Mock private ActuatorRepository actuatorRepository;
//    @Mock private LocationsRepository locationsRepository;
//    @Mock private GroupMembersService groupMembersService;
//
//    @InjectMocks
//    private ActuatorServiceImpl actuatorsService;
//
//    private GroupMembers manager() {
//        return GroupMembers.builder().userId(1L).groupRole(GroupRole.MANAGER).build();
//    }
//
//    private GroupMembers plainMember() {
//        return GroupMembers.builder().userId(1L).groupRole(GroupRole.MEMBER).build();
//    }
//
//    @Test
//    @DisplayName("createActuator - locationId가 groupsId 소속이 아니면 LocationNotFoundException")
//    void 생성_다른그룹location_거부() {
//        given(groupMembersService.validateGroupMembers(10L, 1L)).willReturn(manager());
//        given(locationsRepository.findByLocationIdAndGroups_GroupId(99L, 10L)).willReturn(Optional.empty());
//
//        ActuatorRequest request = new ActuatorRequest(99L, "에어컨", ActuatorType.AIRCON, Map.of("power", "OFF"));
//
//        assertThrows(LocationNotFoundException.class,
//                () -> actuatorsService.createActuator(1L, 10L, request));
//
//        verify(actuatorRepository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("createActuator - MEMBER 권한이면 거부")
//    void 생성_권한없음() {
//        given(groupMembersService.validateGroupMembers(10L, 1L)).willReturn(plainMember());
//
//        ActuatorRequest request = new ActuatorRequest(1L, "에어컨", ActuatorType.AIRCON, Map.of("power", "OFF"));
//
//        assertThrows(com.insighton.core.groups.exception.NoPermissionException.class,
//                () -> actuatorsService.createActuator(1L, 10L, request));
//    }
//
//    @Test
//    @DisplayName("getActuatorById - 다른 그룹 소속 액추에이터면 존재하지 않는 것처럼 404")
//    void 조회_다른그룹_소속아니면_404() {
//        Locations location = mock(Locations.class);
//        given(location.getLocationId()).willReturn(50L);
//        Actuator entity = Actuator.builder().actuatorId(1L).locationId(location).build();
//
//        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
//        given(groupMembersService.validateGroupMembers(10L, 1L)).willReturn(manager());
//        // groupsId=10 소속인지 확인했더니 없음 -> 다른 그룹 리소스
//        given(locationsRepository.findByLocationIdAndGroups_GroupId(50L, 10L)).willReturn(Optional.empty());
//
//        assertThrows(ActuatorNotFoundException.class,
//                () -> actuatorsService.getActuatorById(1L, 10L, 1L));
//    }
//
//    @Test
//    @DisplayName("updateActuatorState - isSystemRequest=true면 권한/소유권 체크를 건너뛴다")
//    void 상태변경_시스템요청_권한체크생략() {
//        Locations location = mock(Locations.class);
//        Actuator entity = Actuator.builder().actuatorId(1L).locationId(location).build();
//        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
//
//        actuatorsService.updateActuatorState(null, null, 1L, Map.of("power", "ON"), true);
//
//        verify(groupMembersService, never()).validateGroupMembers(any(), any());
//        verify(locationsRepository, never()).findByLocationIdAndGroups_GroupId(any(), any());
//    }
//
//    @Test
//    @DisplayName("updateActuatorState - 빈 상태값이면 InvalidActuatorValueException")
//    void 상태변경_빈값_거부() {
//        given(groupMembersService.validateGroupMembers(10L, 1L)).willReturn(manager());
//
//        assertThrows(InvalidActuatorValueException.class,
//                () -> actuatorsService.updateActuatorState(1L, 10L, 1L, Map.of(), false));
//    }
//
//    @Test
//    @DisplayName("deleteActuatorById - 소유권 체크 통과 후 정상 삭제")
//    void 삭제_성공() {
//        Locations location = mock(Locations.class);
//        given(location.getLocationId()).willReturn(50L);
//        Actuator entity = Actuator.builder().actuatorId(1L).locationId(location).build();
//
//        given(actuatorRepository.findById(1L)).willReturn(Optional.of(entity));
//        given(groupMembersService.validateGroupMembers(10L, 1L)).willReturn(manager());
//        given(locationsRepository.findByLocationIdAndGroups_GroupId(50L, 10L)).willReturn(Optional.of(location));
//
//        actuatorsService.deleteActuatorById(1L, 10L, 1L);
//
//        verify(actuatorRepository).delete(entity);
//    }
//
//    @Test
//    @DisplayName("deleteAll - groupsId 소속 location 범위로만 삭제 스코프 제한")
//    void 전체삭제_그룹스코프() {
//        given(groupMembersService.validateGroupMembers(10L, 1L)).willReturn(manager());
//        given(locationsRepository.findAllByGroups_GroupId(10L)).willReturn(java.util.List.of());
//
//        actuatorsService.deleteAll(1L, 10L);
//
//        verify(actuatorRepository).deleteAllByLocationId_LocationIdIn(java.util.List.of());
//    }
//}