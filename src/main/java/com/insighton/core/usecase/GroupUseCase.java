package com.insighton.core.usecase;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.dashboards.entity.Dashboard;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.gateway.service.GatewayService;
import com.insighton.core.domain.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.domain.groups.dto.request.GroupRequest;
import com.insighton.core.domain.groups.dto.response.GroupResponse;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.event.GroupDeletedEvent;
import com.insighton.core.domain.groups.exception.NoPermissionException;
import com.insighton.core.domain.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.domain.sensors.service.SensorService;
import com.insighton.core.domain.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class GroupUseCase {
    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final LocationService locationService;
    private final GatewayService gatewayService;
    private final ApplicationEventPublisher eventPublisher;
    private final DashboardService dashboardService;
    private final WidgetService widgetService;
    private final SensorService sensorService;
    private final GroupRegistrationService groupRegistrationService;

    // ====================== Group Controller ======================

    /**
     * 그룹 가입
     *
     * @param request 그룹 가입 요청 DTO
     */
    @Transactional
    public void joinGroupByToken(GroupMemberJoinRequest request) {
        // inviteToken으로 대상 그룹이 존재하는지 확인 및 조회
        Group group = groupService.validateGroupByInviteToken(request.inviteToken());

        // 그룹 신청서가 대기중 상태인 것이 존재하면 가입 불가능 예외(AlreadyRequestedException)
        groupRegistrationService.validateNoPendingRequest(request.userId());

        groupMemberService.joinGroupByToken(group, request);
    }

    /**
     * 그룹 생성 시 SUPER_MANAGER로 등록되는 (아무나 생성할 수 없도록 제약을 걸어야 함 - 신청서 작성 받기 (group 이름, 사업자 등록 번호 등등))
     *
     * @param groupsCreateRequest 그룹 생성 요청 정보
     * @param userId              그룹을 생성하는 user의 ID
     */
    @Transactional
    public void createGroup(GroupRequest groupsCreateRequest, Long userId) {
        Group group = groupService.createGroup(groupsCreateRequest);

        groupMemberService.createGroupMember(group, userId);
    }


    /**
     * 그룹 수정
     *
     * @param request Group 수정 요청 정보
     * @param userId  login한 user의 ID
     * @param groupId 수정하려는 group의 ID
     */
    @Transactional
    public void updateGroup(GroupRequest request, Long userId, Long groupId) {
        if (groupMemberService.isGroupAdmin(groupId, userId)) {
            groupService.updateGroup(request, groupId);
            return;
        }
        throw new UnAuthorizedAccessException(userId);
    }

    /**
     * 일반 사용자의 초대한 그룹 조회용 (초대장 느낌)
     * 이 초대 토큰은 A 회사의 초대 토큰 입니다 하고 회사 정보를 띄우기.(토큰은 null 값으로 들어감)
     *
     * @param inviteToken 초대 코드
     * @param userId      로그인한 user의 ID
     * @param groupId     내가 지금 보고 있는 group의 ID
     * @return 토큰 값 빼고 group 정보가 들어감
     * return 하기 전에 validateUserExists로 검증
     */
    @Transactional(readOnly = true)
    public GroupResponse getGroupPreview(String inviteToken, Long userId, Long groupId) {
        // 유저가 존재하는지 검증(다른 그룹에 가입이 안 되어있어야함 )
        groupMemberService.validateUserNotInAnyGroup(userId);

        return groupService.getGroupPreview(inviteToken, groupId);
    }

    /**
     * 현재 로그인한 사용자의 소속 그룹 정보 조회
     * (한 계정당 하나의 그룹만 가입 가능하므로, 로그인 정보 기반으로 해당 그룹 정보를 반환)
     *
     * @param userId login한 user의 ID
     * @return token 정보를 제외한 group의 정보
     */
    @Transactional(readOnly = true)
    public GroupResponse getMyGroup(Long userId, Long groupId) {
        // 이건 group안에 user가 속해있는지 같이 보기 위한 메서드임...
        GroupMember members = groupMemberService.validateGroupMembers(groupId, userId);

        // 관리자나 그룹 생성자일 때는
        if (members.isManager() || members.isSuperManager()) {
            return GroupResponse.ofAdmin(members.getGroup());
        }

        // 대상 그룹 조회 (없을 시 exception 던지기)
        return GroupResponse.ofPublic(members.getGroup());
    }


    /**
     * 토큰 재발급
     *
     * @param groupId 재발급 하려는 group의 ID
     * @param userId  재발급 하려는 user의 ID
     */
    @Transactional
    public void newInviteToken(Long userId, Long groupId) {
        GroupMember groupMember = groupMemberService.validateGroupMembers(groupId, userId);

        validationIsAdmin(groupMember);

        groupService.newInviteToken(groupId);
    }

    /**
     * 그룹 삭제
     * 초대 토큰을 입력 받아서 초대토큰이 맞다면 삭제(확인용)
     * 하위에서부터 차근차근 싹 다 삭제한 후에 group까지 delete하기
     * flow등은 삭제 요청 날리기 group이 삭제할거라고? 될거ㅏㄹ고?
     * <p>
     * ON DELETE CASCADE << 이거 사용해서 지우라고??
     *
     * @param userId  그룺을 삭제할 권한을 가진 userID
     * @param groupId 삭제될 group ID
     */
    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        GroupMember groupMember = groupMemberService.validateGroupMembers(groupId, userId);

        if (!groupMember.isSuperManager()) {
            throw NoPermissionException.forAdmin(groupMember.getGroupMemberId());
        }
        gatewayService.deleteByGroupId(groupId);

        sensorService.deleteAll(userId, groupId);

        List<Long> locationIds = locationService.getLocationListByGroupId(groupId).stream()
                .map(Location::getLocationId)
                .toList();

        deleteLocationAll(groupId);

        groupMemberService.deleteGroupMemberAll(userId, groupId);

        groupService.deleteGroup(groupId);

        eventPublisher.publishEvent(new GroupDeletedEvent(groupId, locationIds));
    }

    /**
     * group이 삭제될 때 location도 전부 삭제
     *
     * @param groupId 삭제될 group ID
     */
    public void deleteLocationAll(Long groupId) {
        List<Location> locationList = locationService.getLocationListByGroupId(groupId);
        // dashboards 지우는 로직 추가
        for (Location location : locationList) {
            Long locationId = location.getLocationId();

            // dashboard 다 삭제해주기
            dashboardDelete(locationId);
        }
        // dashboards 삭제
        locationService.deleteLocationAll(groupId);
    }


    /**
     * dashboard delete
     * < @Transactional 안 붙인 이유는 붙이면 이거 호출해서 이 CoreManageMentUseCase 클래스 내에서 노란줄 뜸
     * 삭제용
     *
     * @param locationId 삭제할 location ID
     */
    private void dashboardDelete(Long locationId) {
        Dashboard dashboard = dashboardService.getDashboardEntity(locationId);

        widgetService.deleteAllWidget(dashboard.getDashboardId());

        dashboardService.deleteDashboard(locationId);
    }

    /**
     * member가 관리자인지 확인
     */
    private void validationIsAdmin(GroupMember groupMember) {
        if (groupMember.isMember()) {
            throw NoPermissionException.forAdmin(groupMember.getGroupMemberId());
        }
    }
}
