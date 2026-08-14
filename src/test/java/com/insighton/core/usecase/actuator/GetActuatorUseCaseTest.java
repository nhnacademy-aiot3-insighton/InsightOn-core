package com.insighton.core.usecase.actuator;

import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import com.insighton.core.domain.actuators.exception.ActuatorNotFoundException;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetActuatorUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private ActuatorService actuatorService;

    @InjectMocks
    private GetActuatorUseCase getActuatorUseCase;

    @Test
    @DisplayName("액추에이터 조회 성공 - 그룹 멤버면 조회 가능")
    void getActuatorById_success() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 10L;
        ActuatorResponse expected = mock(ActuatorResponse.class);

        given(actuatorService.getActuatorById(groupId, actuatorId)).willReturn(expected);

        ActuatorResponse result = getActuatorUseCase.getActuatorById(userId, groupId, actuatorId);

        assertThat(result).isEqualTo(expected);
        verify(groupMemberService).validateGroupMembers(groupId, userId);
    }

    @Test
    @DisplayName("액추에이터 조회 실패 - 다른 그룹 소속이면 존재하지 않는 것처럼 404")
    void getActuatorById_fail_notFound() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 999L;

        given(actuatorService.getActuatorById(groupId, actuatorId))
                .willThrow(new ActuatorNotFoundException(actuatorId));

        assertThatThrownBy(() -> getActuatorUseCase.getActuatorById(userId, groupId, actuatorId))
                .isInstanceOf(ActuatorNotFoundException.class);
    }

    @Test
    @DisplayName("액추에이터 조회 실패 - 그룹 멤버가 아니면 조회 안 함")
    void getActuatorById_fail_notMember() {
        Long userId = 999L;
        Long groupId = 5L;
        Long actuatorId = 10L;

        given(groupMemberService.validateGroupMembers(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        assertThatThrownBy(() -> getActuatorUseCase.getActuatorById(userId, groupId, actuatorId))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verifyNoInteractions(actuatorService);
    }
}
