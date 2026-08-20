package com.insighton.core.usecase.actuator;

import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import com.insighton.core.domain.actuators.service.ActuatorService;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetActuatorsByLocationDeleteUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private ActuatorService actuatorService;

    @InjectMocks
    private GetActuatorsByLocationUseCase getActuatorsByLocationUseCase;

    @Test
    @DisplayName("위치별 액추에이터 목록 조회 성공 - 그룹 멤버면 조회 가능")
    void getActuatorsByLocationId_success() {
        Long userId = 1L;
        Long groupId = 5L;
        Long locationId = 20L;
        List<ActuatorResponse> expected = List.of(mock(ActuatorResponse.class));

        given(actuatorService.getActuatorsByLocationId(groupId, locationId)).willReturn(expected);

        List<ActuatorResponse> result = getActuatorsByLocationUseCase.getActuatorsByLocationId(userId, groupId, locationId);

        assertThat(result).isEqualTo(expected);
        verify(groupMemberService).validateGroupMembers(groupId, userId);
    }

    @Test
    @DisplayName("위치별 액추에이터 목록 조회 실패 - 위치가 다른 그룹 소속이면 404")
    void getActuatorsByLocationId_fail_locationNotFound() {
        Long userId = 1L;
        Long groupId = 5L;
        Long locationId = 999L;

        given(actuatorService.getActuatorsByLocationId(groupId, locationId))
                .willThrow(LocationNotFoundException.notFoundLocationByLocationId(locationId));

        assertThatThrownBy(() -> getActuatorsByLocationUseCase.getActuatorsByLocationId(userId, groupId, locationId))
                .isInstanceOf(LocationNotFoundException.class);
    }

    @Test
    @DisplayName("위치별 액추에이터 목록 조회 실패 - 그룹 멤버가 아니면 조회 안 함")
    void getActuatorsByLocationId_fail_notMember() {
        Long userId = 999L;
        Long groupId = 5L;
        Long locationId = 20L;

        given(groupMemberService.validateGroupMembers(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        assertThatThrownBy(() -> getActuatorsByLocationUseCase.getActuatorsByLocationId(userId, groupId, locationId))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verifyNoInteractions(actuatorService);
    }
}
