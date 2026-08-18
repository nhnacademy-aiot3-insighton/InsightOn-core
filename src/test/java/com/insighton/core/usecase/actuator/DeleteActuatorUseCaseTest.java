package com.insighton.core.usecase.actuator;

import com.insighton.core.domain.actuators.event.ActuatorDeletedEvent;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeleteActuatorUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private ActuatorService actuatorService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeleteActuatorUseCase deleteActuatorUseCase;

    @Test
    @DisplayName("액추에이터 삭제 성공 - 매니저 이상 권한이면 삭제 가능, 룰엔진에 삭제 이벤트 발행")
    void deleteActuatorById_success() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 10L;

        deleteActuatorUseCase.deleteActuatorById(userId, groupId, actuatorId);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verify(actuatorService).deleteActuatorById(groupId, actuatorId);
        verify(eventPublisher).publishEvent(new ActuatorDeletedEvent(actuatorId));
    }

    @Test
    @DisplayName("액추에이터 삭제 실패 - MEMBER 권한이면 NoPermissionException, 삭제 안 함")
    void deleteActuatorById_fail_noPermission() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 10L;

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        assertThatThrownBy(() -> deleteActuatorUseCase.deleteActuatorById(userId, groupId, actuatorId))
                .isInstanceOf(NoPermissionException.class);

        verifyNoInteractions(actuatorService);
    }
}
