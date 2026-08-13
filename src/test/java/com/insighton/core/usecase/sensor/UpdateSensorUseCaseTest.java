package com.insighton.core.usecase.sensor;

import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.sensors.dto.SensorUpdateRequest;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.sensors.service.SensorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateSensorUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private SensorService sensorService;

    @InjectMocks
    private UpdateSensorUseCase updateSensorUseCase;

    @Test
    @DisplayName("센서 수정 성공 - 매니저 이상 권한이면 수정 가능")
    void updateSensor_success() {
        Long userId = 1L;
        Long sensorId = 10L;
        Long groupId = 5L;
        SensorUpdateRequest request = new SensorUpdateRequest("4층", "새이름");

        given(sensorService.getSensorGroupId(sensorId)).willReturn(groupId);

        updateSensorUseCase.updateSensor(userId, sensorId, request);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verify(sensorService).updateSensor(sensorId, request);
    }

    @Test
    @DisplayName("센서 수정 실패 - 없는 센서면 권한 검증 전에 SensorNotFoundException")
    void updateSensor_fail_sensorNotFound() {
        Long userId = 1L;
        Long sensorId = 999L;
        SensorUpdateRequest request = new SensorUpdateRequest("4층", null);

        given(sensorService.getSensorGroupId(sensorId)).willThrow(new SensorNotFoundException(sensorId));

        assertThatThrownBy(() -> updateSensorUseCase.updateSensor(userId, sensorId, request))
                .isInstanceOf(SensorNotFoundException.class);

        verifyNoInteractions(groupMemberService);
        verify(sensorService, never()).updateSensor(any(), any());
    }

    @Test
    @DisplayName("센서 수정 실패 - MEMBER 권한이면 NoPermissionException, 수정 안 함")
    void updateSensor_fail_noPermission() {
        Long userId = 1L;
        Long sensorId = 10L;
        Long groupId = 5L;
        SensorUpdateRequest request = new SensorUpdateRequest("4층", null);

        given(sensorService.getSensorGroupId(sensorId)).willReturn(groupId);
        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        assertThatThrownBy(() -> updateSensorUseCase.updateSensor(userId, sensorId, request))
                .isInstanceOf(NoPermissionException.class);

        verify(sensorService, never()).updateSensor(any(), any());
    }

    @Test
    @DisplayName("센서 수정 실패 - 그룹 멤버가 아니면 GroupMemberNotFoundException")
    void updateSensor_fail_notMember() {
        Long userId = 999L;
        Long sensorId = 10L;
        Long groupId = 5L;
        SensorUpdateRequest request = new SensorUpdateRequest("4층", null);

        given(sensorService.getSensorGroupId(sensorId)).willReturn(groupId);
        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        assertThatThrownBy(() -> updateSensorUseCase.updateSensor(userId, sensorId, request))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verify(sensorService, never()).updateSensor(any(), any());
    }
}
