package com.insighton.core.usecase.actuator;

import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UpdateActuatorNameUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private ActuatorService actuatorService;

    @InjectMocks
    private UpdateActuatorNameUseCase updateActuatorNameUseCase;

    @Test
    @DisplayName("이름 수정 성공 - 매니저 이상 권한이면 수정 가능")
    void updateActuatorName_success() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 10L;

        updateActuatorNameUseCase.updateActuatorName(userId, groupId, actuatorId, "새 이름");

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verify(actuatorService).updateActuatorName(groupId, actuatorId, "새 이름");
    }

    @Test
    @DisplayName("이름 수정 실패 - MEMBER 권한이면 NoPermissionException, 수정 안 함")
    void updateActuatorName_fail_noPermission() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 10L;

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        assertThatThrownBy(() -> updateActuatorNameUseCase.updateActuatorName(userId, groupId, actuatorId, "새 이름"))
                .isInstanceOf(NoPermissionException.class);

        verifyNoInteractions(actuatorService);
    }
}
