package com.insighton.core.usecase.actuator;

import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogResponse;
import com.insighton.core.domain.actuatorrunlogs.service.ActuatorRunLogService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetActuatorRunLogsUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private ActuatorService actuatorService;

    @Mock
    private ActuatorRunLogService actuatorRunLogService;

    @InjectMocks
    private GetActuatorRunLogsUseCase getActuatorRunLogsUseCase;

    @Test
    @DisplayName("실행 이력 조회 성공 - getActuatorById로 소유권 검증 겸용")
    void getActuatorRunLogs_success() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 10L;
        Pageable pageable = PageRequest.of(0, 20);
        Page<ActuatorRunLogResponse> expected = Page.empty();

        given(actuatorService.getActuatorById(groupId, actuatorId)).willReturn(mock(ActuatorResponse.class));
        given(actuatorRunLogService.getRunLogsByActuatorId(actuatorId, pageable)).willReturn(expected);

        Page<ActuatorRunLogResponse> result = getActuatorRunLogsUseCase.getActuatorRunLogs(userId, groupId, actuatorId, pageable);

        assertThat(result).isEqualTo(expected);
        verify(groupMemberService).validateGroupMembers(groupId, userId);
    }

    @Test
    @DisplayName("실행 이력 조회 실패 - 다른 그룹 소속 액추에이터면 404, 로그 조회 안 함")
    void getActuatorRunLogs_fail_actuatorNotFound() {
        Long userId = 1L;
        Long groupId = 5L;
        Long actuatorId = 999L;
        Pageable pageable = PageRequest.of(0, 20);

        given(actuatorService.getActuatorById(groupId, actuatorId))
                .willThrow(new ActuatorNotFoundException(actuatorId));

        assertThatThrownBy(() -> getActuatorRunLogsUseCase.getActuatorRunLogs(userId, groupId, actuatorId, pageable))
                .isInstanceOf(ActuatorNotFoundException.class);

        verifyNoInteractions(actuatorRunLogService);
    }

    @Test
    @DisplayName("실행 이력 조회 실패 - 그룹 멤버가 아니면 조회 안 함")
    void getActuatorRunLogs_fail_notMember() {
        Long userId = 999L;
        Long groupId = 5L;
        Long actuatorId = 10L;
        Pageable pageable = PageRequest.of(0, 20);

        given(groupMemberService.validateGroupMembers(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        assertThatThrownBy(() -> getActuatorRunLogsUseCase.getActuatorRunLogs(userId, groupId, actuatorId, pageable))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verifyNoInteractions(actuatorService);
        verifyNoInteractions(actuatorRunLogService);
    }
}
