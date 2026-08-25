package com.insighton.core.usecase.sensor;

import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.sensors.dto.SensorResponse;
import com.insighton.core.domain.sensors.dto.SensorUpdateRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SearchSensorUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private SensorService sensorService;

    @InjectMocks
    private SearchSensorUseCase searchSensorUseCase;

    @Test
    @DisplayName("센서 검색 성공 - 그룹 멤버면 조회 가능")
    void searchSensors_success() {
        Long userId = 1L;
        Long groupId = 5L;
        SensorUpdateRequest request = new SensorUpdateRequest(null, null);
        List<SensorResponse> expected = List.of(
                new SensorResponse(1L, 2L, 3L, "EUI-1", "센서A", OffsetDateTime.now(), OffsetDateTime.now()));

        given(sensorService.searchSensors(groupId, null, null, request)).willReturn(expected);

        List<SensorResponse> result = searchSensorUseCase.searchSensors(userId, groupId, null, null, request);

        assertThat(result).isEqualTo(expected);
        verify(groupMemberService).validateGroupMembers(groupId, userId);
    }

    @Test
    @DisplayName("센서 검색 실패 - 그룹 멤버가 아니면 조회 안 함")
    void searchSensors_fail_notMember() {
        Long userId = 999L;
        Long groupId = 5L;
        SensorUpdateRequest request = new SensorUpdateRequest(null, null);

        given(groupMemberService.validateGroupMembers(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        assertThatThrownBy(() -> searchSensorUseCase.searchSensors(userId, groupId, null, null, request))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verifyNoInteractions(sensorService);
    }
}
