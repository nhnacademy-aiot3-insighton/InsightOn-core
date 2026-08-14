package com.insighton.core.usecase.sensorattribute;

import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.sensorattributes.service.SensorAttributeService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeleteAttributeUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private SensorAttributeService sensorAttributeService;

    @Mock
    private SensorService sensorService;

    @InjectMocks
    private DeleteAttributeUseCase deleteAttributeUseCase;

    @Test
    @DisplayName("속성 삭제 성공 - 매니저 이상 권한이면 삭제 가능")
    void deleteAttribute_success() {
        Long userId = 1L;
        Long sensorId = 10L;
        Long groupId = 5L;
        String metricKey = "co2";

        given(sensorService.getSensorGroupId(sensorId)).willReturn(groupId);

        deleteAttributeUseCase.deleteAttribute(userId, sensorId, metricKey);

        verify(groupMemberService).validateGroupAdmin(groupId, userId);
        verify(sensorAttributeService).deleteAttribute(sensorId, metricKey);
    }

    @Test
    @DisplayName("속성 삭제 실패 - 센서가 없으면 SensorNotFoundException, 권한 검증 안 함")
    void deleteAttribute_fail_sensorNotFound() {
        Long userId = 1L;
        Long sensorId = 999L;
        String metricKey = "co2";

        given(sensorService.getSensorGroupId(sensorId)).willThrow(new SensorNotFoundException(sensorId));

        assertThatThrownBy(() -> deleteAttributeUseCase.deleteAttribute(userId, sensorId, metricKey))
                .isInstanceOf(SensorNotFoundException.class);

        verifyNoInteractions(groupMemberService);
        verifyNoInteractions(sensorAttributeService);
    }

    @Test
    @DisplayName("속성 삭제 실패 - MEMBER 권한이면 NoPermissionException, 삭제 안 함")
    void deleteAttribute_fail_noPermission() {
        Long userId = 1L;
        Long sensorId = 10L;
        Long groupId = 5L;
        String metricKey = "co2";

        given(sensorService.getSensorGroupId(sensorId)).willReturn(groupId);
        given(groupMemberService.validateGroupAdmin(groupId, userId))
                .willThrow(NoPermissionException.forAdmin(userId));

        assertThatThrownBy(() -> deleteAttributeUseCase.deleteAttribute(userId, sensorId, metricKey))
                .isInstanceOf(NoPermissionException.class);

        verifyNoInteractions(sensorAttributeService);
    }
}
