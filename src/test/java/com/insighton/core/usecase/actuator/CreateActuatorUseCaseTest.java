package com.insighton.core.usecase.actuator;

import com.insighton.core.domain.actuators.dto.ActuatorRequest;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CreateActuatorUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private ActuatorService actuatorService;

    @InjectMocks
    private CreateActuatorUseCase createActuatorUseCase;

    @Test
    @DisplayName("액추에이터 생성 성공 - 매니저 이상 권한이면 생성 가능")
    void createActuator_success() {
        Long userId = 1L;
        Long groupId = 5L;
        ActuatorRequest request = new ActuatorRequest("회의실", "에어컨", ActuatorType.AIRCON, Map.of("power", "OFF"));

        given(actuatorService.createActuator(groupId, request)).willReturn(100L);

        Long result = createActuatorUseCase.createActuator(userId, groupId, request);

        assertThat(result).isEqualTo(100L);
        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verify(actuatorService).createActuator(groupId, request);
    }

    @Test
    @DisplayName("액추에이터 생성 실패 - MEMBER 권한이면 NoPermissionException, 생성 안 함")
    void createActuator_fail_noPermission() {
        Long userId = 1L;
        Long groupId = 5L;
        ActuatorRequest request = new ActuatorRequest("회의실", "에어컨", ActuatorType.AIRCON, Map.of("power", "OFF"));

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        assertThatThrownBy(() -> createActuatorUseCase.createActuator(userId, groupId, request))
                .isInstanceOf(NoPermissionException.class);

        verifyNoInteractions(actuatorService);
    }

    @Test
    @DisplayName("액추에이터 생성 실패 - 그룹 멤버가 아니면 GroupMemberNotFoundException")
    void createActuator_fail_notMember() {
        Long userId = 999L;
        Long groupId = 5L;
        ActuatorRequest request = new ActuatorRequest("회의실", "에어컨", ActuatorType.AIRCON, Map.of("power", "OFF"));

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        assertThatThrownBy(() -> createActuatorUseCase.createActuator(userId, groupId, request))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verifyNoInteractions(actuatorService);
    }
}
