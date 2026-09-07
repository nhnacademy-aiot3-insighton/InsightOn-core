package com.insighton.core.usecase.sensor;

import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.sensors.service.SensorService;
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
class DeleteAllSensorUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private SensorService sensorService;

    @InjectMocks
    private DeleteAllSensorUseCase deleteAllSensorUseCase;

    @Test
    @DisplayName("전체 삭제 성공 - 매니저 이상 권한이면 삭제 가능")
    void deleteAll_success() {
        Long userId = 1L;
        Long groupId = 5L;

        deleteAllSensorUseCase.deleteAll(userId, groupId);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verify(sensorService).deleteAll(groupId);
    }

    @Test
    @DisplayName("전체 삭제 실패 - MEMBER 권한이면 NoPermissionException, 삭제 안 함")
    void deleteAll_fail_noPermission() {
        Long userId = 1L;
        Long groupId = 5L;

        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        assertThatThrownBy(() -> deleteAllSensorUseCase.deleteAll(userId, groupId))
                .isInstanceOf(NoPermissionException.class);

        verifyNoInteractions(sensorService);
    }
}
