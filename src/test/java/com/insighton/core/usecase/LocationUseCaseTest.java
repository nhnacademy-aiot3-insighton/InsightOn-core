package com.insighton.core.usecase;

import com.insighton.core.dashboards.entity.Dashboard;
import com.insighton.core.dashboards.service.DashboardService;
import com.insighton.core.groupmember.entity.GroupMember;
import com.insighton.core.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.groupmember.service.GroupMemberService;
import com.insighton.core.groups.entity.Group;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.location.dto.request.LocationCreateRequest;
import com.insighton.core.location.dto.request.LocationUpdateRequest;
import com.insighton.core.location.dto.response.LocationListResponse;
import com.insighton.core.location.dto.response.LocationResponse;
import com.insighton.core.location.entity.Location;
import com.insighton.core.location.service.LocationService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationUseCaseTest {

    @Mock
    private LocationService locationService;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private DashboardService dashboardService;


    @InjectMocks
    private LocationUseCase managementUseCase;

    @Nested
    @DisplayName("location test code")
    class LocationTest {

        @Test
        @DisplayName("Location 생성 성공")
        void createLocation_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            LocationCreateRequest request = new LocationCreateRequest("test", Location.AutoControlMode.SUGGESTION);

            Group mockGroup = mock(Group.class);
            GroupMember mockGroupMember = mock(GroupMember.class);

            given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMember);

            given(mockGroupMember.isMember()).willReturn(false);
            given(mockGroupMember.getGroup()).willReturn(mockGroup);

            Location mockLocation = mock(Location.class);
            given(mockLocation.getLocationId()).willReturn(1L);
            given(locationService.createLocation(mockGroup, request)).willReturn(mockLocation);

            // when
            managementUseCase.createLocation(userId, groupId, request);

            // then
            verify(groupMemberService, times(1)).validateGroupMembers(groupId, userId);
            verify(locationService, times(1)).createLocation(mockGroup, request);
            verify(dashboardService, times(1)).createDashboard(any(), any());
        }

        @Test
        @DisplayName("Location 생성 실패 - 존재하지 않는 그룹인 경우 예외 발생")
        void createLocation_fail_groupNotFound() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            LocationCreateRequest request = new LocationCreateRequest("test", Location.AutoControlMode.SUGGESTION);

            given(groupMemberService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> managementUseCase.createLocation(userId, groupId, request))
                    .isInstanceOf(GroupMemberNotFoundException.class);
        }

        @Test
        @DisplayName("Location 생성 실패 - 그룹에 속하지 않은 사용자인 경우 예외 발생")
        void createLocation_fail_memberNotFound() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            LocationCreateRequest request = new LocationCreateRequest("test", Location.AutoControlMode.SUGGESTION);

            given(groupMemberService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> managementUseCase.createLocation(userId, groupId, request))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMemberService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService);
        }

        @Test
        @DisplayName("Location 생성 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
        void createLocation_fail_noPermission() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            LocationCreateRequest request = new LocationCreateRequest("test", Location.AutoControlMode.SUGGESTION);

            GroupMember mockGroupMember = mock(GroupMember.class);

            given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMember);

            given(mockGroupMember.isMember()).willReturn(true);
            given(mockGroupMember.getGroupMemberId()).willReturn(10L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.createLocation(userId, groupId, request))
                    .isInstanceOf(NoPermissionException.class);

            verify(groupMemberService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService);
        }

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
            List<LocationListResponse> result = managementUseCase.getLocationList(userId, groupId);

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
            assertThatThrownBy(() -> managementUseCase.getLocationList(userId, groupId))
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
            LocationResponse result = managementUseCase.getLocation(userId, groupId, locationId);

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
            assertThatThrownBy(() -> managementUseCase.getLocation(userId, groupId, locationId))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMemberService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService);
        }

        // ==================== 로케이션 모드 수정 ====================

        @Test
        @DisplayName("Location 모드 수정 성공 - 관리자 권한 확인 후 변경")
        void toggleAutoControlMode_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 10L;

            GroupMember mockGroupMember = mock(GroupMember.class);
            given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMember);
            given(mockGroupMember.isMember()).willReturn(false);

            // when
            managementUseCase.toggleAutoControlMode(userId, groupId, locationId);

            // then
            verify(groupMemberService).validateGroupMembers(groupId, userId);
            verify(locationService).toggleAutoControlMode(locationId, groupId);
        }

        @Test
        @DisplayName("Location 모드 수정 실패 - 그룹에 유저가 존재하지 않는 경우 예외 발생")
        void toggleAutoControlMode_fail_memberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;
            Long locationId = 10L;

            given(groupMemberService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> managementUseCase.toggleAutoControlMode(userId, groupId, locationId))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMemberService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService);
        }

        @Test
        @DisplayName("Location 모드 수정 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
        void toggleAutoControlMode_fail_noPermission() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 10L;

            GroupMember mockGroupMember = mock(GroupMember.class);
            given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMember);
            given(mockGroupMember.isMember()).willReturn(true);
            given(mockGroupMember.getGroupMemberId()).willReturn(10L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.toggleAutoControlMode(userId, groupId, locationId))
                    .isInstanceOf(NoPermissionException.class);

            verify(groupMemberService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService);
        }

        // ==================== 로케이션 이름 수정 ====================

        @Test
        @DisplayName("Location 이름 수정 성공 - 관리자 권한 확인 후 수정")
        void updateName_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long targetLocationId = 10L;
            LocationUpdateRequest request = new LocationUpdateRequest("새이름");

            GroupMember mockGroupMember = mock(GroupMember.class);
            given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMember);
            given(mockGroupMember.isMember()).willReturn(false);

            Location mockLocation = mock(Location.class);
            given(mockLocation.getLocationId()).willReturn(targetLocationId);
            given(locationService.getLocationByGroupId(targetLocationId, groupId)).willReturn(mockLocation);

            // when
            managementUseCase.updateName(userId, groupId, targetLocationId, request);

            // then
            verify(groupMemberService).validateGroupMembers(groupId, userId);
            verify(locationService).updateName(targetLocationId, groupId, request);
        }

        @Test
        @DisplayName("Location 이름 수정 실패 - 그룹에 유저가 존재하지 않는 경우 예외 발생")
        void updateName_fail_memberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;
            Long targetLocationId = 10L;
            LocationUpdateRequest request = new LocationUpdateRequest("새이름");

            given(groupMemberService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> managementUseCase.updateName(userId, groupId, targetLocationId, request))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMemberService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService);
        }

        @Test
        @DisplayName("Location 이름 수정 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
        void updateName_fail_noPermission() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long targetLocationId = 10L;
            LocationUpdateRequest request = new LocationUpdateRequest("새이름");

            GroupMember mockGroupMember = mock(GroupMember.class);
            given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMember);
            given(mockGroupMember.isMember()).willReturn(true);
            given(mockGroupMember.getGroupMemberId()).willReturn(10L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.updateName(userId, groupId, targetLocationId, request))
                    .isInstanceOf(NoPermissionException.class);

            verify(groupMemberService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService);
        }

        // ==================== 로케이션 삭제 ====================

        @Test
        @DisplayName("Location 삭제 성공 - 관리자 권한 확인 후 삭제")
        void deleteLocation_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long targetLocationId = 10L;

            Dashboard mockDashboard = Dashboard.builder().build();

            given(dashboardService.getDashboardEntity(anyLong()))
                    .willReturn(mockDashboard);
            GroupMember mockGroupMember = mock(GroupMember.class);
            given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMember);
            given(mockGroupMember.isMember()).willReturn(false);

            // when
            managementUseCase.deleteLocation(userId, groupId, targetLocationId);

            // then
            verify(groupMemberService).validateGroupMembers(groupId, userId);
            verify(locationService).deleteLocation(targetLocationId, groupId);
        }

        @Test
        @DisplayName("Location 삭제 실패 - 삭제를 시도하는 사람이 그룹에 존재하지 않는 경우 예외 발생")
        void deleteLocation_fail_memberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;
            Long targetLocationId = 10L;

            given(groupMemberService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> managementUseCase.deleteLocation(userId, groupId, targetLocationId))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMemberService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService);
        }

        @Test
        @DisplayName("Location 삭제 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
        void deleteLocation_fail_noPermission() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long targetLocationId = 10L;

            GroupMember mockGroupMember = mock(GroupMember.class);
            given(groupMemberService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMember);
            given(mockGroupMember.isMember()).willReturn(true);
            given(mockGroupMember.getGroupMemberId()).willReturn(10L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.deleteLocation(userId, groupId, targetLocationId))
                    .isInstanceOf(NoPermissionException.class);

            verify(groupMemberService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationService);
        }

    }
}
