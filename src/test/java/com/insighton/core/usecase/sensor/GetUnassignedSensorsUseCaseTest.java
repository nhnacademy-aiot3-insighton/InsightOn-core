package com.insighton.core.usecase.sensor;

import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.service.SensorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetUnassignedSensorsUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private SensorService sensorService;

    @InjectMocks
    private GetUnassignedSensorsUseCase getUnassignedSensorsUseCase;

    @Test
    @DisplayName("장소 미배정 센서 조회 성공 - 그룹 멤버 검증 통과 후 위임")
    void getUnassignedSensors_success() {
        Long userId = 1L;
        Long groupId = 5L;
        SensorResponse response = new SensorResponse(10L, 20L, null, "EUI-1", "센서A", OffsetDateTime.now(), OffsetDateTime.now());
        given(sensorService.getUnassignedSensors(groupId)).willReturn(List.of(response));

        List<SensorResponse> result = getUnassignedSensorsUseCase.getUnassignedSensors(userId, groupId);

        assertThat(result).containsExactly(response);
        verify(groupMemberService).validateGroupMembers(groupId, userId);
    }

    @Test
    @DisplayName("장소 미배정 센서 조회 실패 - 그룹 멤버가 아니면 조회 자체를 안 함")
    void getUnassignedSensors_fail_notMember() {
        Long userId = 999L;
        Long groupId = 5L;
        given(groupMemberService.validateGroupMembers(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        assertThatThrownBy(() -> getUnassignedSensorsUseCase.getUnassignedSensors(userId, groupId))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verify(sensorService, never()).getUnassignedSensors(anyLong());
    }
}
