package com.insighton.core.groups.service;

import com.insighton.core.gateway.service.GatewayService;
import com.insighton.core.groupmember.dto.request.GroupMembersJoinRequest;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.exception.AlreadyJoinedException;
import com.insighton.core.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.dto.request.GroupsRequest;
import com.insighton.core.groups.dto.response.GroupsResponse;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.exception.GroupNotFoundException;
import com.insighton.core.groups.exception.InviteTokenNotFoundException;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.location.dto.request.LocationsCreateRequest;
import com.insighton.core.location.dto.request.LocationsUpdateRequest;
import com.insighton.core.location.dto.response.LocationsListResponse;
import com.insighton.core.location.dto.response.LocationsResponse;
import com.insighton.core.location.entity.Locations;
import com.insighton.core.location.service.LocationsService;
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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupManagementUseCaseTest {

    @Mock
    private GroupsService groupService;

    @Mock
    private GatewayService gatewayService;

    @Mock
    private LocationsService locationsService;

    @Mock
    private GroupMembersService groupMembersService;

    @InjectMocks
    private GroupManagementUseCase managementUseCase;

    // ==================== 그룹 가입 ====================

    @Nested
    @DisplayName("Group test code")
    class GroupTest {
        @Test
        @DisplayName("그룹 가입 성공")
        void joinGroupByToken_success() {
            // given
            GroupMembersJoinRequest request = new GroupMembersJoinRequest("token", 1L);
            Groups mockGroup = mock(Groups.class);
            given(groupService.validateGroupByInviteToken("token")).willReturn(mockGroup);

            // when
            managementUseCase.joinGroupByToken(request);

            // then
            verify(groupMembersService, times(1)).joinGroupByToken(mockGroup, request);
        }

        @Test
        @DisplayName("그룹 가입 실패 - 존재하지 않는 초대 토큰")
        void joinGroupByToken_notFoundGroup() {
            // given
            GroupMembersJoinRequest request = new GroupMembersJoinRequest("bad-token", 1L);
            given(groupService.validateGroupByInviteToken("bad-token")).willThrow(InviteTokenNotFoundException.class);

            // when & then
            assertThatThrownBy(() -> managementUseCase.joinGroupByToken(request))
                    .isInstanceOf(InviteTokenNotFoundException.class);
        }

        @Test
        @DisplayName("그룹 가입 실패 - 이미 그룹에 가입된 유저")
        void joinGroupByToken_alreadyJoined() {
            // given
            GroupMembersJoinRequest request = new GroupMembersJoinRequest("token", 1L);
            Groups mockGroup = mock(Groups.class);
            given(groupService.validateGroupByInviteToken("token")).willReturn(mockGroup);
            willThrow(new AlreadyJoinedException(1L))
                    .given(groupMembersService).joinGroupByToken(mockGroup, request);

            // when & then
            assertThatThrownBy(() -> managementUseCase.joinGroupByToken(request))
                    .isInstanceOf(AlreadyJoinedException.class);
        }

        // ==================== 그룹 생성 ====================

        @Test
        @DisplayName("그룹 생성 성공")
        void createGroup_success() {
            // given
            GroupsRequest request = new GroupsRequest("name", "desc", "loc");
            Groups mockGroup = mock(Groups.class);
            given(groupService.createGroup(request)).willReturn(mockGroup);

            // when
            managementUseCase.createGroup(request, 1L);

            // then
            verify(groupMembersService, times(1)).createGroupMember(mockGroup, 1L);
        }

        @Test
        @DisplayName("그룹 생성 실패 - 이미 다른 그룹에 가입되어 있어 멤버 생성이 불가능할 때")
        void createGroup_alreadyJoined() {
            // given
            GroupsRequest request = new GroupsRequest("name", "desc", "loc");
            Groups mockGroup = mock(Groups.class);
            given(groupService.createGroup(request)).willReturn(mockGroup);
            willThrow(new AlreadyJoinedException(1L))
                    .given(groupMembersService).createGroupMember(mockGroup, 1L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.createGroup(request, 1L))
                    .isInstanceOf(AlreadyJoinedException.class);
        }

        // ==================== 그룹 수정 ====================

        @Test
        @DisplayName("그룹 수정 성공 - 어드민 권한 확인")
        void updateGroup_success() {
            // given
            GroupsRequest request = new GroupsRequest("name", "desc", "loc");
            given(groupMembersService.isGroupAdmin(1L, 1L)).willReturn(true);

            // when
            managementUseCase.updateGroup(request, 1L, 1L);

            // then
            verify(groupService, times(1)).updateGroup(request, 1L);
        }

        @Test
        @DisplayName("그룹 수정 실패 - 어드민 권한 없음")
        void updateGroup_notAdmin() {
            // given
            GroupsRequest request = new GroupsRequest("name", "desc", "loc");
            given(groupMembersService.isGroupAdmin(1L, 1L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> managementUseCase.updateGroup(request, 1L, 1L))
                    .isInstanceOf(UnAuthorizedAccessException.class);
        }

        // ==================== 그룹 조회 (미리보기 & 내 그룹) ====================

        @Test
        @DisplayName("초대 그룹 프리뷰 조회 성공")
        void getGroupPreview_success() {
            // given
            given(groupService.getGroupPreview("token", 1L)).willReturn(mock(GroupsResponse.class));

            // when
            GroupsResponse response = managementUseCase.getGroupPreview("token", 1L, 1L);

            // then
            assertThat(response).isNotNull();
            verify(groupMembersService, times(1)).validateUserNotInAnyGroup(1L);
        }

        @Test
        @DisplayName("초대 그룹 프리뷰 조회 실패 - 이미 가입된 유저인 경우")
        void getGroupPreview_alreadyJoined() {
            // given
            willThrow(new AlreadyJoinedException(1L))
                    .given(groupMembersService).validateUserNotInAnyGroup(1L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.getGroupPreview("token", 1L, 1L))
                    .isInstanceOf(AlreadyJoinedException.class);
        }

        @Test
        @DisplayName("내 그룹 정보 조회 성공 - 관리자 권한 응답 반환")
        void getMyGroup_success() {
            // given
            GroupMembers mockMember = mock(GroupMembers.class);
            Groups mockGroup = mock(Groups.class);
            given(mockMember.getGroups()).willReturn(mockGroup);
            given(mockMember.isManager()).willReturn(true);
            given(groupMembersService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

            // when
            GroupsResponse response = managementUseCase.getMyGroup(1L, 1L);

            // then
            assertThat(response).isNotNull();
            verify(mockMember, times(1)).isManager();
        }

        @Test
        @DisplayName("내 그룹 정보 조회 실패 - 해당 그룹의 멤버가 아님")
        void getMyGroup_notFoundMember() {
            // given
            given(groupMembersService.validateGroupMembers(1L, 1L))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(1L, 1L));

            // when & then
            assertThatThrownBy(() -> managementUseCase.getMyGroup(1L, 1L))
                    .isInstanceOf(GroupMemberNotFoundException.class);
        }

        // ==================== 토큰 재발급 & 삭제 ====================

        @Test
        @DisplayName("토큰 재발급 성공 - 관리자 권한 확인")
        void newInviteToken_success() {
            // given
            GroupMembers mockMember = mock(GroupMembers.class);
            given(mockMember.isMember()).willReturn(false);
            given(groupMembersService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

            // when
            managementUseCase.newInviteToken(1L, 1L);

            // then
            verify(groupService, times(1)).newInviteToken(1L);
        }

        @Test
        @DisplayName("토큰 재발급 실패 - 일반 멤버 권한으로 시도 시")
        void newInviteToken_notAdmin() {
            // given
            GroupMembers mockMember = mock(GroupMembers.class);
            given(mockMember.isMember()).willReturn(true);
            given(mockMember.getGroupMemberId()).willReturn(10L);
            given(groupMembersService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

            // when & then
            assertThatThrownBy(() -> managementUseCase.newInviteToken(1L, 1L))
                    .isInstanceOf(NoPermissionException.class);
        }

        @Test
        @DisplayName("그룹 삭제 성공 - 슈퍼 매니저만 가능하며 연관 멤버 및 그룹 데이터 모두 삭제")
        void deleteGroup_success() {
            // given
            GroupMembers mockMember = mock(GroupMembers.class);
            given(mockMember.isSuperManager()).willReturn(true);
            given(groupMembersService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

            // when
            managementUseCase.deleteGroup(1L, 1L);

            // then
            verify(gatewayService, times(1)).deleteByGroupId(1L);
            verify(groupMembersService, times(1)).deleteGroupMemberAll(1L, 1L); // 멤버 전체 삭제 검증
            verify(locationsService, times(1)).deleteLocationAll(1L);
            verify(groupService, times(1)).deleteGroup(1L); // 그룹 엔티티 삭제 검증
        }

        @Test
        @DisplayName("그룹 삭제 실패 - 슈퍼 매니저가 아닐 때")
        void deleteGroup_notSuperManager() {
            // given
            GroupMembers mockMember = mock(GroupMembers.class);
            given(mockMember.isSuperManager()).willReturn(false);
            given(mockMember.getGroupMemberId()).willReturn(10L);
            given(groupMembersService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

            // when & then
            assertThatThrownBy(() -> managementUseCase.deleteGroup(1L, 1L))
                    .isInstanceOf(NoPermissionException.class);
        }
    }


    @Nested
    @DisplayName("location test code")
    class LocationTest {

        @Test
        @DisplayName("Location 생성 성공")
        void createLocation_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            LocationsCreateRequest request = new LocationsCreateRequest("test", Locations.AutoControlMode.SUGGESTION);

            Groups mockGroup = mock(Groups.class);
            GroupMembers mockGroupMembers = mock(GroupMembers.class);


            given(groupService.groupFindById(groupId)).willReturn(mockGroup);
            given(groupMembersService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMembers);

            given(mockGroupMembers.isMember()).willReturn(false);

            // when
            managementUseCase.createLocation(userId, groupId, request);

            // then
            verify(groupService, times(1)).groupFindById(groupId);
            verify(groupMembersService, times(1)).validateGroupMembers(groupId, userId);
            verify(locationsService, times(1)).createLocation(mockGroup, request);
        }

        @Test
        @DisplayName("Location 생성 실패 - 존재하지 않는 그룹인 경우 예외 발생")
        void createLocation_fail_groupNotFound() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            LocationsCreateRequest request = new LocationsCreateRequest("test", Locations.AutoControlMode.SUGGESTION);

            given(groupService.groupFindById(groupId))
                    .willThrow(GroupNotFoundException.class);

            // when & then
            assertThatThrownBy(() -> managementUseCase.createLocation(userId, groupId, request))
                    .isInstanceOf(GroupNotFoundException.class);

            verify(groupService).groupFindById(groupId);
            verifyNoInteractions(groupMembersService, locationsService);
        }

        @Test
        @DisplayName("Location 생성 실패 - 그룹에 속하지 않은 사용자인 경우 예외 발생")
        void createLocation_fail_memberNotFound() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            LocationsCreateRequest request = new LocationsCreateRequest("test", Locations.AutoControlMode.SUGGESTION);

            Groups mockGroup = mock(Groups.class);
            given(groupService.groupFindById(groupId)).willReturn(mockGroup);
            given(groupMembersService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> managementUseCase.createLocation(userId, groupId, request))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupService).groupFindById(groupId);
            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationsService);
        }

        @Test
        @DisplayName("Location 생성 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
        void createLocation_fail_noPermission() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            LocationsCreateRequest request = new LocationsCreateRequest("test", Locations.AutoControlMode.SUGGESTION);

            Groups mockGroup = mock(Groups.class);
            GroupMembers mockGroupMembers = mock(GroupMembers.class);

            given(groupService.groupFindById(groupId)).willReturn(mockGroup);
            given(groupMembersService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMembers);

            given(mockGroupMembers.isMember()).willReturn(true);
            given(mockGroupMembers.getGroupMemberId()).willReturn(10L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.createLocation(userId, groupId, request))
                    .isInstanceOf(NoPermissionException.class);

            verify(groupService).groupFindById(groupId);
            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationsService);
        }

        //        로케이션 리스트 조회 성공, 실패(user가 존재하지 않음)
        @Test
        @DisplayName("Location List 조회 성공")
        void getLocationList_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;

            GroupMembers mockGroupMembers = mock(GroupMembers.class);
            LocationsListResponse response1 = new LocationsListResponse(1L, "거실", Locations.AutoControlMode.SUGGESTION);
            LocationsListResponse response2 = new LocationsListResponse(2L, "안방", Locations.AutoControlMode.SUGGESTION);
            List<LocationsListResponse> expectedResponse = List.of(response1, response2);

            given(groupMembersService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMembers);
            given(locationsService.getLocationList(groupId)).willReturn(expectedResponse);

            // when
            List<LocationsListResponse> result = managementUseCase.getLocationList(userId, groupId);

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result).isEqualTo(expectedResponse);

            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verify(locationsService).getLocationList(groupId);
        }

        @Test
        @DisplayName("Location List 조회 실패 - 존재하지 않는 유저(그룹 멤버)인 경우 예외 발생")
        void getLocationList_fail_memberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;

            given(groupMembersService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> managementUseCase.getLocationList(userId, groupId))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verify(locationsService, times(0)).getLocationList(groupId);
        }


        // ==================== 로케이션 상세 정보 조회 ====================

        @Test
        @DisplayName("Location 상세 정보 조회 성공")
        void getLocation_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 10L;

            GroupMembers mockGroupMembers = mock(GroupMembers.class);
            LocationsResponse expectedResponse = new LocationsResponse(locationId, groupId, "거실", OffsetDateTime.now(), Locations.AutoControlMode.SUGGESTION);

            given(groupMembersService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMembers);
            given(locationsService.getLocation(locationId, groupId)).willReturn(expectedResponse);

            // when
            LocationsResponse result = managementUseCase.getLocation(userId, groupId, locationId);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expectedResponse);

            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verify(locationsService).getLocation(locationId, groupId);
        }

        @Test
        @DisplayName("Location 상세 정보 조회 실패 - 존재하지 않는 유저(그룹 멤버)인 경우 예외 발생")
        void getLocation_fail_memberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;
            Long locationId = 10L;

            given(groupMembersService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> managementUseCase.getLocation(userId, groupId, locationId))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationsService);
        }

        // ==================== 로케이션 모드 수정 ====================

        @Test
        @DisplayName("Location 모드 수정 성공 - 관리자 권한 확인 후 변경")
        void toggleAutoControlMode_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 10L;

            GroupMembers mockGroupMembers = mock(GroupMembers.class);
            given(groupMembersService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMembers);
            given(mockGroupMembers.isMember()).willReturn(false);

            // when
            managementUseCase.toggleAutoControlMode(userId, groupId, locationId);

            // then
            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verify(locationsService).toggleAutoControlMode(locationId, groupId);
        }

        @Test
        @DisplayName("Location 모드 수정 실패 - 그룹에 유저가 존재하지 않는 경우 예외 발생")
        void toggleAutoControlMode_fail_memberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;
            Long locationId = 10L;

            given(groupMembersService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> managementUseCase.toggleAutoControlMode(userId, groupId, locationId))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationsService);
        }

        @Test
        @DisplayName("Location 모드 수정 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
        void toggleAutoControlMode_fail_noPermission() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long locationId = 10L;

            GroupMembers mockGroupMembers = mock(GroupMembers.class);
            given(groupMembersService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMembers);
            given(mockGroupMembers.isMember()).willReturn(true);
            given(mockGroupMembers.getGroupMemberId()).willReturn(10L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.toggleAutoControlMode(userId, groupId, locationId))
                    .isInstanceOf(NoPermissionException.class);

            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationsService);
        }

        // ==================== 로케이션 이름 수정 ====================

        @Test
        @DisplayName("Location 이름 수정 성공 - 관리자 권한 확인 후 수정")
        void updateName_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long targetLocationId = 10L;
            LocationsUpdateRequest request = new LocationsUpdateRequest("새이름");

            GroupMembers mockGroupMembers = mock(GroupMembers.class);
            given(groupMembersService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMembers);
            given(mockGroupMembers.isMember()).willReturn(false);

            // when
            managementUseCase.updateName(userId, groupId, targetLocationId, request);

            // then
            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verify(locationsService).updateName(targetLocationId, groupId, request);
        }

        @Test
        @DisplayName("Location 이름 수정 실패 - 그룹에 유저가 존재하지 않는 경우 예외 발생")
        void updateName_fail_memberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;
            Long targetLocationId = 10L;
            LocationsUpdateRequest request = new LocationsUpdateRequest("새이름");

            given(groupMembersService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> managementUseCase.updateName(userId, groupId, targetLocationId, request))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationsService);
        }

        @Test
        @DisplayName("Location 이름 수정 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
        void updateName_fail_noPermission() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long targetLocationId = 10L;
            LocationsUpdateRequest request = new LocationsUpdateRequest("새이름");

            GroupMembers mockGroupMembers = mock(GroupMembers.class);
            given(groupMembersService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMembers);
            given(mockGroupMembers.isMember()).willReturn(true);
            given(mockGroupMembers.getGroupMemberId()).willReturn(10L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.updateName(userId, groupId, targetLocationId, request))
                    .isInstanceOf(NoPermissionException.class);

            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationsService);
        }

        // ==================== 로케이션 삭제 ====================

        @Test
        @DisplayName("Location 삭제 성공 - 관리자 권한 확인 후 삭제")
        void deleteLocation_success() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long targetLocationId = 10L;

            GroupMembers mockGroupMembers = mock(GroupMembers.class);
            given(groupMembersService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMembers);
            given(mockGroupMembers.isMember()).willReturn(false);

            // when
            managementUseCase.deleteLocation(userId, groupId, targetLocationId);

            // then
            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verify(locationsService).deleteLocation(targetLocationId, groupId);
        }

        @Test
        @DisplayName("Location 삭제 실패 - 삭제를 시도하는 사람이 그룹에 존재하지 않는 경우 예외 발생")
        void deleteLocation_fail_memberNotFound() {
            // given
            Long userId = 999L;
            Long groupId = 1L;
            Long targetLocationId = 10L;

            given(groupMembersService.validateGroupMembers(groupId, userId))
                    .willThrow(GroupMemberNotFoundException.byUserIdAndGroupId(userId, groupId));

            // when & then
            assertThatThrownBy(() -> managementUseCase.deleteLocation(userId, groupId, targetLocationId))
                    .isInstanceOf(GroupMemberNotFoundException.class);

            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationsService);
        }

        @Test
        @DisplayName("Location 삭제 실패 - 일반 MEMBER 권한인 경우 NoPermissionException 발생")
        void deleteLocation_fail_noPermission() {
            // given
            Long userId = 100L;
            Long groupId = 1L;
            Long targetLocationId = 10L;

            GroupMembers mockGroupMembers = mock(GroupMembers.class);
            given(groupMembersService.validateGroupMembers(groupId, userId)).willReturn(mockGroupMembers);
            given(mockGroupMembers.isMember()).willReturn(true);
            given(mockGroupMembers.getGroupMemberId()).willReturn(10L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.deleteLocation(userId, groupId, targetLocationId))
                    .isInstanceOf(NoPermissionException.class);

            verify(groupMembersService).validateGroupMembers(groupId, userId);
            verifyNoInteractions(locationsService);
        }

        // ==================== 로케이션 모두 삭제 ====================

        @Test
        @DisplayName("Location 모두 삭제 성공")
        void deleteLocationAll_success() {
            // given
            Long groupId = 1L;

            // when
            managementUseCase.deleteLocationAll(groupId);

            // then
            verify(locationsService).deleteLocationAll(groupId);
        }
    }
}