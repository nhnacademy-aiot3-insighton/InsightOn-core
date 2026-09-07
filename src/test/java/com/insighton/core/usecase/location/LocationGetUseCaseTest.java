package com.insighton.core.usecase.location;

import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.location.dto.response.LocationListResponse;
import com.insighton.core.domain.location.dto.response.LocationResponse;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.service.LocationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LocationGetUseCaseTest {

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private LocationGetUseCase locationGetUseCase;

    @Nested
    @DisplayName("location 목록 조회 케이스")
    class GetLocationListTest {

        @Test
        @DisplayName("location 목록 조회 성공 - 그룹 멤버 검증 후 목록 반환")
        void getLocationList_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;

            LocationListResponse response = LocationListResponse.builder()
                    .locationId(10L)
                    .locationName("4층 개발팀")
                    .autoControlMode(Location.AutoControlMode.SUGGESTION)
                    .build();

            given(locationService.getLocationList(groupId)).willReturn(List.of(response));

            // when
            List<LocationListResponse> result = locationGetUseCase.getLocationList(userId, groupId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).locationId()).isEqualTo(10L);

            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
            verify(locationService, times(1)).getLocationList(groupId);
        }

        @Test
        @DisplayName("location 목록 조회 실패 - 그룹에 속하지 않은 사용자인 경우 예외 발생")
        void getLocationList_fail_groupMemberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;

            given(groupMemberService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> locationGetUseCase.getLocationList(userId, groupId))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService);
        }
    }

    @Nested
    @DisplayName("location 상세 조회 케이스")
    class GetLocationTest {

        @Test
        @DisplayName("location 상세 조회 성공 - 그룹 멤버 검증 후 상세 정보 반환")
        void getLocation_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 10L;

            LocationResponse response = LocationResponse.builder()
                    .locationId(locationId)
                    .groupId(groupId)
                    .locationName("4층 개발팀")
                    .createdAt(OffsetDateTime.now())
                    .autoControlMode(Location.AutoControlMode.SUGGESTION)
                    .build();

            given(locationService.getLocation(locationId, groupId)).willReturn(response);

            // when
            LocationResponse result = locationGetUseCase.getLocation(userId, groupId, locationId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.locationId()).isEqualTo(locationId);
            assertThat(result.locationName()).isEqualTo("4층 개발팀");

            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
            verify(locationService, times(1)).getLocation(locationId, groupId);
        }

        @Test
        @DisplayName("location 상세 조회 실패 - 그룹에 속하지 않은 사용자인 경우 예외 발생")
        void getLocation_fail_groupMemberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;
            Long locationId = 10L;

            given(groupMemberService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> locationGetUseCase.getLocation(userId, groupId, locationId))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService);
        }
    }
}
