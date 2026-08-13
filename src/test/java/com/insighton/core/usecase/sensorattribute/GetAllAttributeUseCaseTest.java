package com.insighton.core.usecase.sensorattribute;

import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.sensorattributes.dto.SensorAttributeResponse;
import com.insighton.core.domain.sensorattributes.service.SensorAttributeService;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.domain.sensors.service.SensorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetAllAttributeUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private SensorAttributeService sensorAttributeService;

    @Mock
    private SensorService sensorService;

    @InjectMocks
    private GetAllAttributeUseCase getAllAttributeUseCase;

    @Test
    @DisplayName("속성 전체 조회 성공 - 그룹 멤버면 조회 가능")
    void getAllAttributeBySensorId_success() {
        Long userId = 1L;
        Long sensorId = 10L;
        Long groupId = 5L;
        List<SensorAttributeResponse> expected = List.of(new SensorAttributeResponse("co2", "이산화탄소", "ppm"));

        given(sensorService.getSensorGroupId(sensorId)).willReturn(groupId);
        given(sensorAttributeService.getAllAttributeBySensorId(sensorId)).willReturn(expected);

        List<SensorAttributeResponse> result = getAllAttributeUseCase.getAllAttributeBySensorId(userId, sensorId);

        assertThat(result).isEqualTo(expected);
        verify(groupMemberService).validateGroupMembers(groupId, userId);
    }

    @Test
    @DisplayName("속성 전체 조회 실패 - 센서가 없으면 SensorNotFoundException, 권한 검증 안 함")
    void getAllAttributeBySensorId_fail_sensorNotFound() {
        Long userId = 1L;
        Long sensorId = 999L;

        given(sensorService.getSensorGroupId(sensorId)).willThrow(new SensorNotFoundException(sensorId));

        assertThatThrownBy(() -> getAllAttributeUseCase.getAllAttributeBySensorId(userId, sensorId))
                .isInstanceOf(SensorNotFoundException.class);

        verifyNoInteractions(groupMemberService);
        verifyNoInteractions(sensorAttributeService);
    }

    @Test
    @DisplayName("속성 전체 조회 실패 - 그룹 멤버가 아니면 조회 안 함")
    void getAllAttributeBySensorId_fail_notMember() {
        Long userId = 999L;
        Long sensorId = 10L;
        Long groupId = 5L;

        given(sensorService.getSensorGroupId(sensorId)).willReturn(groupId);
        given(groupMemberService.validateGroupMembers(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        assertThatThrownBy(() -> getAllAttributeUseCase.getAllAttributeBySensorId(userId, sensorId))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verifyNoInteractions(sensorAttributeService);
    }
}
