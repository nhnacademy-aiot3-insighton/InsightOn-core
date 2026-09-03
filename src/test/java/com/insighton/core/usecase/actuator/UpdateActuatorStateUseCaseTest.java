package com.insighton.core.usecase.actuator;

import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UpdateActuatorStateUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private ActuatorControlFacade actuatorControlFacade;

    @InjectMocks
    private UpdateActuatorStateUseCase updateActuatorStateUseCase;

    @Test
    @DisplayName("성공 - 매니저 이상 권한 검증 후 Facade에 위임 (항상 USER + 호출자 userId)")
    void updateActuatorState_success() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 10L;
        Map<String, Object> newState = Map.of("power", "ON");

        updateActuatorStateUseCase.updateActuatorState(userId, groupId, actuatorId, newState);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verify(actuatorControlFacade).control(groupId, actuatorId, newState, ExecutedByType.USER, userId);
    }

    @Test
    @DisplayName("실패 - MEMBER 권한이면 NoPermissionException, Facade 호출 안 함")
    void updateActuatorState_fail_noPermission() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 10L;
        Map<String, Object> newState = Map.of("power", "ON");

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        assertThatThrownBy(() -> updateActuatorStateUseCase.updateActuatorState(userId, groupId, actuatorId, newState))
                .isInstanceOf(NoPermissionException.class);

        verifyNoInteractions(actuatorControlFacade);
    }

    @Test
    @DisplayName("실패 - Facade가 던진 예외(빈 상태 등)는 그대로 전파")
    void updateActuatorState_fail_facadeThrows() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 10L;
        Map<String, Object> newState = Map.of();

        willThrow(new InvalidActuatorValueException("액추에이터 제어 상태 값(newState)은 비어있음"))
                .given(actuatorControlFacade).control(groupId, actuatorId, newState, ExecutedByType.USER, userId);

        assertThatThrownBy(() -> updateActuatorStateUseCase.updateActuatorState(userId, groupId, actuatorId, newState))
                .isInstanceOf(InvalidActuatorValueException.class);
    }
}
