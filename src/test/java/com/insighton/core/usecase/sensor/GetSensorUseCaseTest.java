package com.insighton.core.usecase.sensor;

import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.sensors.service.SensorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetSensorUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private SensorService sensorService;

    @InjectMocks
    private GetSensorUseCase getSensorUseCase;

    @Test
    @DisplayName("센서 조회 성공 - 그룹 멤버면 조회 가능")
    void getSensorById_success() {
        Long userId = 1L;
        Long sensorId = 10L;
        Long groupId = 5L;
        SensorResponse expected = new SensorResponse(sensorId, 20L, 30L, "EUI-1", "센서A", OffsetDateTime.now(), OffsetDateTime.now());

        given(sensorService.getSensorGroupId(sensorId)).willReturn(groupId);
        given(sensorService.getSensorById(sensorId)).willReturn(expected);

        SensorResponse result = getSensorUseCase.getSensorById(userId, sensorId);

        assertThat(result).isEqualTo(expected);
        verify(groupMemberService).validateGroupMembers(groupId, userId);
        verify(sensorService).getSensorById(sensorId);
    }

    @Test
    @DisplayName("센서 조회 실패 - 없는 센서면 그룹 검증 전에 SensorNotFoundException")
    void getSensorById_fail_sensorNotFound() {
        Long userId = 1L;
        Long sensorId = 999L;

        given(sensorService.getSensorGroupId(sensorId)).willThrow(new SensorNotFoundException(sensorId));

        assertThatThrownBy(() -> getSensorUseCase.getSensorById(userId, sensorId))
                .isInstanceOf(SensorNotFoundException.class);

        verifyNoInteractions(groupMemberService);
        verify(sensorService, never()).getSensorById(any());
    }

    @Test
    @DisplayName("센서 조회 실패 - 그룹 멤버가 아니면 GroupMemberNotFoundException, 조회 안 함")
    void getSensorById_fail_notMember() {
        Long userId = 999L;
        Long sensorId = 10L;
        Long groupId = 5L;

        given(sensorService.getSensorGroupId(sensorId)).willReturn(groupId);
        given(groupMemberService.validateGroupMembers(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        assertThatThrownBy(() -> getSensorUseCase.getSensorById(userId, sensorId))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verify(sensorService, never()).getSensorById(any());
    }
}
