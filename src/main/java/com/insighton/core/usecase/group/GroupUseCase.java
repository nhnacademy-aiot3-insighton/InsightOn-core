package com.insighton.core.usecase.group;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.dashboards.service.DashboardService;
import com.insighton.core.domain.gateway.service.GatewayService;
import com.insighton.core.domain.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.domain.groups.dto.request.GroupRequest;
import com.insighton.core.domain.groups.dto.request.GroupUpdateRequest;
import com.insighton.core.domain.groups.dto.response.GroupResponse;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.event.GroupRegionUpdateEvent;
import com.insighton.core.domain.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.domain.region.service.RegionService;
import com.insighton.core.domain.sensors.service.SensorService;
import com.insighton.core.domain.widgets.service.WidgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

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
    private final RegionService regionService;

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
    public Group createGroup(GroupRequest groupsCreateRequest, Long userId) {
        Group group = groupService.createGroup(groupsCreateRequest);

        groupMemberService.createGroupMember(group, userId);
        return group;
    }


    /**
     * 그룹 수정
     *
     * @param request Group 수정 요청 정보
     * @param userId  login한 user의 ID
     * @param groupId 수정하려는 group의 ID
     */
    @Transactional
    public void updateGroup(GroupUpdateRequest request, Long userId, Long groupId) {
        if (groupMemberService.isGroupAdmin(groupId, userId)) {
            groupService.updateGroup(request, groupId);

            if (request.groupRegion() != null) {
                eventPublisher.publishEvent(new GroupRegionUpdateEvent(groupId, request.groupRegion()));
            }
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
        groupMemberService.validateGroupAdmin(groupId, userId);

        groupService.newInviteToken(groupId);
    }
}
