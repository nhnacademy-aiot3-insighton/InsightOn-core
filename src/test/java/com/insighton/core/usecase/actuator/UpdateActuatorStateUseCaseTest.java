package com.insighton.core.usecase.actuator;

import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.exception.InvalidActuatorValueException;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
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
    private ActuatorService actuatorService;

    @InjectMocks
    private UpdateActuatorStateUseCase updateActuatorStateUseCase;

    @Test
    @DisplayName("상태 변경 성공 - 매니저 이상 권한이면 변경 가능, 항상 USER+호출자ID로 기록")
    void updateActuatorState_success() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 10L;
        Map<String, Object> newState = Map.of("power", "ON");

        given(actuatorService.getActuatorById(groupId, actuatorId))
                .willReturn(new ActuatorResponse(actuatorId, 1L, "테스트 에어컨", ActuatorType.AIRCON,
                        Map.of(), OffsetDateTime.now(), OffsetDateTime.now()));

        updateActuatorStateUseCase.updateActuatorState(userId, groupId, actuatorId, newState);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verify(actuatorService).updateActuatorState(groupId, actuatorId, newState, ExecutedByType.USER, userId);
    }

    @Test
    @DisplayName("상태 변경 실패 - MEMBER 권한이면 NoPermissionException, 변경 안 함")
    void updateActuatorState_fail_noPermission() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 10L;
        Map<String, Object> newState = Map.of("power", "ON");

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        assertThatThrownBy(() -> updateActuatorStateUseCase.updateActuatorState(userId, groupId, actuatorId, newState))
                .isInstanceOf(NoPermissionException.class);

        verifyNoInteractions(actuatorService);
    }

    @Test
    @DisplayName("상태 변경 실패 - 빈 상태값이면 Service에서 InvalidActuatorValueException")
    void updateActuatorState_fail_emptyState() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 10L;
        Map<String, Object> newState = Map.of();

        given(actuatorService.getActuatorById(groupId, actuatorId))
                .willReturn(new ActuatorResponse(actuatorId, 1L, "테스트 에어컨", ActuatorType.AIRCON,
                        Map.of(), OffsetDateTime.now(), OffsetDateTime.now()));

        willThrow(new InvalidActuatorValueException("액추에이터 제어 상태 값(newState)은 비어있음"))
                .given(actuatorService).updateActuatorState(groupId, actuatorId, newState, ExecutedByType.USER, userId);

        assertThatThrownBy(() -> updateActuatorStateUseCase.updateActuatorState(userId, groupId, actuatorId, newState))
                .isInstanceOf(InvalidActuatorValueException.class);
    }
}
