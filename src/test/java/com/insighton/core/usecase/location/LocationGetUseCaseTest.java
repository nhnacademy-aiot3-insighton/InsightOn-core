package com.insighton.core.usecase.location;

import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.location.dto.response.LocationListResponse;
import com.insighton.core.domain.location.dto.response.LocationResponse;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.service.LocationService;
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
class LocationGetUseCaseTest {

    @Mock
    private LocationService locationService;

    @Mock
    private GroupMemberService groupMemberService;

    @InjectMocks
    private LocationGetUseCase locationGetUseCase;

    //        로케이션 리스트 조회 성공, 실패(user가 존재하지 않음)
    @Test
    @DisplayName("Location List 조회 성공")
    void getLocationList_success() {
        // given
        Long userId = 100L;
        Long groupId = 1L;

        GroupMember mockGroupMember = mock(GroupMember.class);
        LocationListResponse response1 = new LocationListResponse(1L, "거실", Location.AutoControlMode.SUGGESTION);
        LocationListResponse response2 = new LocationListResponse(2L, "안방", Location.AutoControlMode.SUGGESTION);
        List<LocationListResponse> expectedResponse = List.of(response1, response2);

        given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMember);
        given(locationService.getLocationList(groupId)).willReturn(expectedResponse);

        // when
        List<LocationListResponse> result = locationGetUseCase.getLocationList(userId, groupId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedResponse);

        verify(groupMemberService).validateGroupMembers(groupId, userId);
        verify(locationService).getLocationList(groupId);
    }

    @Test
    @DisplayName("Location List 조회 실패 - 존재하지 않는 유저(그룹 멤버)인 경우 예외 발생")
    void getLocationList_fail_memberNotFound() {
        // given
        Long userId = 999L;
        Long groupId = 1L;

        given(groupMemberService.validateGroupMembers(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        // when & then
        assertThatThrownBy(() -> locationGetUseCase.getLocationList(userId, groupId))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verify(groupMemberService).validateGroupMembers(groupId, userId);
        verify(locationService, times(0)).getLocationList(groupId);
    }


    // ==================== 로케이션 상세 정보 조회 ====================

    @Test
    @DisplayName("Location 상세 정보 조회 성공")
    void getLocation_success() {
        // given
        Long userId = 100L;
        Long groupId = 1L;
        Long locationId = 10L;

        GroupMember mockGroupMember = mock(GroupMember.class);
        LocationResponse expectedResponse = new LocationResponse(locationId, groupId, "거실", OffsetDateTime.now(), Location.AutoControlMode.SUGGESTION);

        given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMember);
        given(locationService.getLocation(locationId, groupId)).willReturn(expectedResponse);

        // when
        LocationResponse result = locationGetUseCase.getLocation(userId, groupId, locationId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedResponse);

        verify(groupMemberService).validateGroupMembers(groupId, userId);
        verify(locationService).getLocation(locationId, groupId);
    }

    @Test
    @DisplayName("Location 상세 정보 조회 실패 - 존재하지 않는 유저(그룹 멤버)인 경우 예외 발생")
    void getLocation_fail_memberNotFound() {
        // given
        Long userId = 999L;
        Long groupId = 1L;
        Long locationId = 10L;

        given(groupMemberService.validateGroupMembers(groupId, userId))
                .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

        // when & then
        assertThatThrownBy(() -> locationGetUseCase.getLocation(userId, groupId, locationId))
                .isInstanceOf(GroupMemberNotFoundException.class);

        verify(groupMemberService).validateGroupMembers(groupId, userId);
        verifyNoInteractions(locationService);
    }

}