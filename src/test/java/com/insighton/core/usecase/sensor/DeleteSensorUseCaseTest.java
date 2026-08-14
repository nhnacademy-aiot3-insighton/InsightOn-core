package com.insighton.core.usecase.sensor;

import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.sensors.service.SensorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteSensorUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private SensorService sensorService;

    @InjectMocks
    private DeleteSensorUseCase deleteSensorUseCase;

    @Test
    @DisplayName("센서 삭제 성공 - 매니저 이상 권한이면 삭제 가능")
    void deleteSensor_success() {
        Long userId = 1L;
        Long sensorId = 10L;
        Long groupId = 5L;

        given(sensorService.getSensorGroupId(sensorId)).willReturn(groupId);

        deleteSensorUseCase.deleteSensor(userId, sensorId);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verify(sensorService).deleteSensor(sensorId);
    }

    @Test
    @DisplayName("센서 삭제 실패 - 없는 센서면 권한 검증 전에 SensorNotFoundException")
    void deleteSensor_fail_sensorNotFound() {
        Long userId = 1L;
        Long sensorId = 999L;

        given(sensorService.getSensorGroupId(sensorId)).willThrow(new SensorNotFoundException(sensorId));

        assertThatThrownBy(() -> deleteSensorUseCase.deleteSensor(userId, sensorId))
                .isInstanceOf(SensorNotFoundException.class);

        verifyNoInteractions(groupMemberService);
        verify(sensorService, never()).deleteSensor(any());
    }

    @Test
    @DisplayName("센서 삭제 실패 - MEMBER 권한이면 NoPermissionException, 삭제 안 함")
    void deleteSensor_fail_noPermission() {
        Long userId = 1L;
        Long sensorId = 10L;
        Long groupId = 5L;

        given(sensorService.getSensorGroupId(sensorId)).willReturn(groupId);
        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        assertThatThrownBy(() -> deleteSensorUseCase.deleteSensor(userId, sensorId))
                .isInstanceOf(NoPermissionException.class);

        verify(sensorService, never()).deleteSensor(any());
    }
}
