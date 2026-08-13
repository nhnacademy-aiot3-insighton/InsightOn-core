package com.insighton.core.usecase;

import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.gateway.service.GatewayService;
import com.insighton.core.domain.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.exception.AlreadyJoinedException;
import com.insighton.core.domain.groupmember.exception.GroupMemberNotFoundException;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groupregistration.exception.AlreadyRequestedException;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.domain.groups.dto.request.GroupRequest;
import com.insighton.core.domain.groups.dto.request.GroupUpdateRequest;
import com.insighton.core.domain.groups.dto.response.GroupResponse;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.event.GroupDeletedEvent;
import com.insighton.core.domain.groups.event.GroupRegionUpdateEvent;
import com.insighton.core.domain.groups.exception.InviteTokenNotFoundException;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.domain.sensors.service.SensorService;
import com.insighton.core.domain.widgets.service.WidgetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupUseCaseTest {

    @Mock
    private GroupService groupService;

    @Mock
    private GatewayService gatewayService;

    @Mock
    private LocationService locationService;

    @Mock
    private GroupMemberService groupMemberService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private WidgetService widgetService;

    @Mock
    private SensorService sensorService;

    @Mock
    private GroupRegistrationService groupRegistrationService;

    @InjectMocks
    private GroupUseCase managementUseCase;

    // ==================== 그룹 가입 ====================

    @Nested
    @DisplayName("Group test code")
    class GroupTest {
        @Test
        @DisplayName("그룹 가입 성공")
        void joinGroupByToken_success() {
            // given
            GroupMemberJoinRequest request = new GroupMemberJoinRequest("token", 1L);
            Group mockGroup = mock(Group.class);
            given(groupService.validateGroupByInviteToken("token")).willReturn(mockGroup);

            // when
            managementUseCase.joinGroupByToken(request);

            // then
            verify(groupMemberService, times(1)).joinGroupByToken(mockGroup, request);
        }

        @Test
        @DisplayName("그룹 가입 실패 - 존재하지 않는 초대 토큰")
        void joinGroupByToken_notFoundGroup() {
            // given
            GroupMemberJoinRequest request = new GroupMemberJoinRequest("bad-token", 1L);
            given(groupService.validateGroupByInviteToken("bad-token")).willThrow(InviteTokenNotFoundException.class);

            // when & then
            assertThatThrownBy(() -> managementUseCase.joinGroupByToken(request))
                    .isInstanceOf(InviteTokenNotFoundException.class);
        }

        @Test
        @DisplayName("그룹 가입 실패 - 이미 그룹에 가입된 유저")
        void joinGroupByToken_alreadyJoined() {
            // given
            GroupMemberJoinRequest request = new GroupMemberJoinRequest("token", 1L);
            Group mockGroup = mock(Group.class);
            given(groupService.validateGroupByInviteToken("token")).willReturn(mockGroup);
            willThrow(new AlreadyJoinedException(1L))
                    .given(groupMemberService).joinGroupByToken(mockGroup, request);

            // when & then
            assertThatThrownBy(() -> managementUseCase.joinGroupByToken(request))
                    .isInstanceOf(AlreadyJoinedException.class);
        }

        @Test
        @DisplayName("그룹 가입 실패 - 승인 대기 중인 신청서가 존재함")
        void joinGroupByToken_alreadyRequested() {
            // given
            GroupMemberJoinRequest request = new GroupMemberJoinRequest("token", 1L);
            Group mockGroup = mock(Group.class);
            given(groupService.validateGroupByInviteToken("token")).willReturn(mockGroup);
            willThrow(AlreadyRequestedException.of())
                    .given(groupRegistrationService).validateNoPendingRequest(1L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.joinGroupByToken(request))
                    .isInstanceOf(AlreadyRequestedException.class);
        }

        // ==================== 그룹 생성 ====================

        @Test
        @DisplayName("그룹 생성 성공")
        void createGroup_success() {
            // given
            GroupRequest request = new GroupRequest("name", "desc", "loc");
            Group mockGroup = mock(Group.class);
            given(groupService.createGroup(request)).willReturn(mockGroup);

            // when
            managementUseCase.createGroup(request, 1L);

            // then
            verify(groupMemberService, times(1)).createGroupMember(mockGroup, 1L);
        }

        @Test
        @DisplayName("그룹 생성 실패 - 이미 다른 그룹에 가입되어 있어 멤버 생성이 불가능할 때")
        void createGroup_alreadyJoined() {
            // given
            GroupRequest request = new GroupRequest("name", "desc", "loc");
            Group mockGroup = mock(Group.class);
            given(groupService.createGroup(request)).willReturn(mockGroup);
            willThrow(new AlreadyJoinedException(1L))
                    .given(groupMemberService).createGroupMember(mockGroup, 1L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.createGroup(request, 1L))
                    .isInstanceOf(AlreadyJoinedException.class);
        }

        // ==================== 그룹 수정 ====================

        @Test
        @DisplayName("그룹 수정 성공 - 어드민 권한 확인")
        void updateGroup_success() {
            // given
            GroupUpdateRequest request = new GroupUpdateRequest("name", "desc", null);
            given(groupMemberService.isGroupAdmin(1L, 1L)).willReturn(true);

            // when
            managementUseCase.updateGroup(request, 1L, 1L);

            // then
            verify(groupService, times(1)).updateGroup(request, 1L);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("그룹 수정 성공 - 지역 정보 변경 시 GroupRegionUpdateEvent 이벤트 발행")
        void updateGroup_success_withRegionEvent() {
            // given
            GroupUpdateRequest request = new GroupUpdateRequest("name", "desc", "서울시 강남구");
            given(groupMemberService.isGroupAdmin(1L, 1L)).willReturn(true);

            // when
            managementUseCase.updateGroup(request, 1L, 1L);

            // then
            verify(groupService, times(1)).updateGroup(request, 1L);
            verify(eventPublisher, times(1)).publishEvent(any(GroupRegionUpdateEvent.class));
        }

        @Test
        @DisplayName("그룹 수정 실패 - 어드민 권한 없음")
        void updateGroup_notAdmin() {
            // given
            GroupUpdateRequest request = new GroupUpdateRequest("name", "desc", "loc");
            given(groupMemberService.isGroupAdmin(1L, 1L)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> managementUseCase.updateGroup(request, 1L, 1L))
                    .isInstanceOf(UnAuthorizedAccessException.class);
        }

        // ==================== 그룹 조회 (미리보기 & 내 그룹) ====================

        @Test
        @DisplayName("초대 그룹 프리뷰 조회 성공")
        void getGroupPreview_success() {
            // given
            given(groupService.getGroupPreview("token", 1L)).willReturn(mock(GroupResponse.class));

            // when
            GroupResponse response = managementUseCase.getGroupPreview("token", 1L, 1L);

            // then
            assertThat(response).isNotNull();
            verify(groupMemberService, times(1)).validateUserNotInAnyGroup(1L);
        }

        @Test
        @DisplayName("초대 그룹 프리뷰 조회 실패 - 이미 가입된 유저인 경우")
        void getGroupPreview_alreadyJoined() {
            // given
            willThrow(new AlreadyJoinedException(1L))
                    .given(groupMemberService).validateUserNotInAnyGroup(1L);

            // when & then
            assertThatThrownBy(() -> managementUseCase.getGroupPreview("token", 1L, 1L))
                    .isInstanceOf(AlreadyJoinedException.class);
        }

        @Test
        @DisplayName("내 그룹 정보 조회 성공 - 관리자 권한 응답 반환")
        void getMyGroup_success() {
            // given
            GroupMember mockMember = mock(GroupMember.class);
            Group mockGroup = mock(Group.class);
            given(mockMember.getGroup()).willReturn(mockGroup);
            given(mockMember.isManager()).willReturn(true);
            given(groupMemberService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

            // when
            GroupResponse response = managementUseCase.getMyGroup(1L, 1L);

            // then
            assertThat(response).isNotNull();
            verify(mockMember, times(1)).isManager();
        }

        @Test
        @DisplayName("내 그룹 정보 조회 실패 - 해당 그룹의 멤버가 아님")
        void getMyGroup_notFoundMember() {
            // given
            given(groupMemberService.validateGroupMembers(1L, 1L))
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
            GroupMember mockMember = mock(GroupMember.class);
            given(mockMember.isMember()).willReturn(false);
            given(groupMemberService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

            // when
            managementUseCase.newInviteToken(1L, 1L);

            // then
            verify(groupService, times(1)).newInviteToken(1L);
        }

        @Test
        @DisplayName("토큰 재발급 실패 - 일반 멤버 권한으로 시도 시")
        void newInviteToken_notAdmin() {
            // given
            GroupMember mockMember = mock(GroupMember.class);
            given(mockMember.isMember()).willReturn(true);
            given(mockMember.getGroupMemberId()).willReturn(10L);
            given(groupMemberService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

            // when & then
            assertThatThrownBy(() -> managementUseCase.newInviteToken(1L, 1L))
                    .isInstanceOf(NoPermissionException.class);
        }

        @Test
        @DisplayName("그룹 삭제 성공 - 슈퍼 매니저만 가능하며 연관 멤버 및 그룹 데이터 모두 삭제")
        void deleteGroup_success() {
            // given
            GroupMember mockMember = mock(GroupMember.class);
            given(mockMember.isSuperManager()).willReturn(true);
            given(groupMemberService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

            // when

            // then
            verify(gatewayService, times(1)).deleteByGroupId(1L);
            verify(sensorService, times(1)).deleteAll(1L, 1L);
            verify(groupMemberService, times(1)).deleteGroupMemberAll(1L, 1L);
            verify(locationService, times(1)).deleteLocationAll(1L);
            verify(groupService, times(1)).deleteGroup(1L);

            verify(eventPublisher, times(1)).publishEvent(any(GroupDeletedEvent.class));
        }

        @Test
        @DisplayName("그룹 삭제 실패 - 슈퍼 매니저가 아닐 때")
        void deleteGroup_notSuperManager() {
            // given
            GroupMember mockMember = mock(GroupMember.class);
            given(mockMember.isSuperManager()).willReturn(false);
            given(mockMember.getGroupMemberId()).willReturn(10L);
            given(groupMemberService.validateGroupMembers(1L, 1L)).willReturn(mockMember);

            String token = "token";

            // when & then
            assertThatThrownBy(() -> managementUseCase.deleteGroup(1L, 1L, token))
                    .isInstanceOf(NoPermissionException.class);
        }

        @Test
        @DisplayName("Location 모두 삭제 성공 - 연결된 대시보드 및 위젯도 삭제")
        void deleteLocationAll_success() {
            // given
            Long groupId = 1L;
            Location mockLocation = mock(Location.class);
            given(mockLocation.getLocationId()).willReturn(10L);
            given(locationService.getLocationListByGroupId(groupId)).willReturn(List.of(mockLocation));

            Dashboard mockDashboard = mock(Dashboard.class);
            given(mockDashboard.getDashboardId()).willReturn(100L);
            given(dashboardService.getDashboardEntity(10L)).willReturn(mockDashboard);

            // when
            managementUseCase.deleteLocationAll(groupId);

            // then
            verify(widgetService, times(1)).deleteAllWidget(100L);
            verify(dashboardService, times(1)).deleteDashboard(10L);
            verify(locationService, times(1)).deleteLocationAll(groupId);
        }
    }


}




