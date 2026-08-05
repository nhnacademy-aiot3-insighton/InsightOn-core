package com.insighton.core.groups.service;

import com.insighton.core.dashboards.dto.request.DashboardRequest;
import com.insighton.core.dashboards.dto.response.DashboardResponse;
import com.insighton.core.dashboards.service.DashboardService;
import com.insighton.core.gateway.service.GatewayService;
import com.insighton.core.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.groupmember.entity.GroupMember;
import com.insighton.core.groupmember.service.GroupMemberService;
import com.insighton.core.groups.dto.request.GroupRequest;
import com.insighton.core.groups.dto.response.GroupResponse;
import com.insighton.core.groups.entity.Group;
import com.insighton.core.groups.event.GroupDeletedEvent;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.location.dto.request.LocationCreateRequest;
import com.insighton.core.location.dto.request.LocationUpdateRequest;
import com.insighton.core.location.dto.response.LocationListResponse;
import com.insighton.core.location.dto.response.LocationResponse;
import com.insighton.core.location.entity.Location;
import com.insighton.core.location.event.LocationDeletedEvent;
import com.insighton.core.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoreManagementUseCase {
    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final LocationService locationService;
    private final GatewayService gatewayService;
    private final ApplicationEventPublisher eventPublisher;
    private final DashboardService dashboardService;

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

        groupMemberService.joinGroupByToken(group, request);
    }

    /**
     * 그룹 생성 시 SUPER_MANAGER로 등록되는
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

        groupMemberService.deleteGroupMemberAll(userId, groupId);

        List<Long> locationIds = locationService.getLocationListByGroupId(groupId).stream()
                .map(Location::getLocationId)
                .toList();

        deleteLocationAll(groupId);

        groupService.deleteGroup(groupId);

        eventPublisher.publishEvent(new GroupDeletedEvent(groupId, locationIds));
    }

    // ====================== Location Controller ======================

    /**
     * location 생성
     *
     * @param userId  location을 생성하려는 user의 ID
     * @param groupId location을 만드려는 group ID
     * @param request location 생성 request
     */
    @Transactional
    public void createLocation(Long userId, Long groupId, LocationCreateRequest request) {
        // 그룹이 존재한다면 그 그룹 안에 location을 만드려는 사람이 존재하는지 확인하고
        GroupMember groupMember = groupMemberService.validateGroupMembers(groupId, userId);

        // member가 member 권한일 때는 에러를 던지고
        validationIsAdmin(groupMember);

        // 만들기
        Location location = locationService.createLocation(groupMember.getGroup(), request);

        DashboardRequest dashboardRequest = new DashboardRequest(location.getLocationId(), request.locationName() + " - dashboard");

        dashboardService.createDashboard(location, dashboardRequest);
    }

    /**
     * location list 조회
     *
     * @param userId  list를 조회하는 user의 ID
     * @param groupId location들이 속해있는 group의 ID
     * @return location List 반환
     */
    @Transactional(readOnly = true)
    public List<LocationListResponse> getLocationList(Long userId, Long groupId) {
        // 그룹에 user가 존재하는지 확인
        groupMemberService.validateGroupMembers(groupId, userId);

        return locationService.getLocationList(groupId);
    }

    /**
     * location 상세 정보 조회
     *
     * @param userId  location의 상세 정보를 조회하려는 User의 ID
     * @param groupId location이 속해있는 group의 ID
     * @return location 상세 정보 반환
     */
    @Transactional(readOnly = true)
    public LocationResponse getLocation(Long userId, Long groupId, Long locationId) {
        // 그룹에 user가 존재하는지 확인
        groupMemberService.validateGroupMembers(groupId, userId);

        return locationService.getLocation(locationId, groupId);
    }

    /**
     * location mode 수정
     *
     * @param userId     정보를 수정하려는 user의 ID
     * @param groupId    수정하려는 location이 속해있는 group의 ID
     * @param locationId 수정하려는 location의 ID
     *                   user의 권한 확인 후 수정
     */
    @Transactional
    public void toggleAutoControlMode(Long userId, Long groupId, Long locationId) {

        GroupMember groupMember = groupMemberService.validateGroupMembers(groupId, userId);

        // member가 member 권한일 때는 에러를 던지고
        validationIsAdmin(groupMember);

        locationService.toggleAutoControlMode(locationId, groupId);
    }

    /**
     * location name 수정
     *
     * @param userId           정보를 수정하려는 user의 ID
     * @param groupId          수정하려는 location이 속해있는 group의 ID
     * @param targetLocationId 수정하려는 location의 ID
     * @param request          수정 할 이름
     */
    @Transactional
    public void updateName(Long userId, Long groupId, Long targetLocationId, LocationUpdateRequest request) {

        GroupMember groupMember = groupMemberService.validateGroupMembers(groupId, userId);

        // member가 member 권한일 때는 에러를 던지고
        validationIsAdmin(groupMember);

        locationService.updateName(targetLocationId, groupId, request);

        Location location = locationService.getLocationByGroupId(targetLocationId, groupId);

        // 이름이 바뀐 location entity 객체를 가져와서 dashboards title에도 적용시켜준다.
        DashboardRequest dashboardRequest = new DashboardRequest(location.getLocationId(), location.getLocationName() + " - dashboard");

        dashboardService.updateDashboardTitle(dashboardRequest);
    }

    /**
     * location 삭제
     * 하위에 있는 것들을 먼저 차근차근 삭제하시오 여기서.
     * 센서 디바이스는 삭제 말고 locations_id를 null 값으로 바꿔주셔?
     *
     * @param userId           삭제하려는 user의 ID
     * @param groupId          삭제될 location이 속해있는 group ID
     * @param targetLocationId 삭제될 location ID
     *                         user의 권한 확인 후 삭제
     */
    @Transactional
    public void deleteLocation(Long userId, Long groupId, Long targetLocationId) {
        // 그룹이 존재한다면 그 그룹 안에 location을 삭제하려는 사람이 존재하는지 확인하고
        GroupMember groupMember = groupMemberService.validateGroupMembers(groupId, userId);

        // member가 member 권한일 때는 에러를 던지고
        validationIsAdmin(groupMember);

        // devices는 location 값만 null로 바꿔주기

        // dashboards 삭제
        dashboardService.deleteDashboard(targetLocationId);

        locationService.deleteLocation(targetLocationId, groupId);

        eventPublisher.publishEvent(new LocationDeletedEvent(targetLocationId));
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
            // devices는 location 값만 null로 바꿔주기

            // dashboard 다 삭제해주기
            dashboardService.deleteDashboard(locationId);
        }
        // dashboards 삭제
        locationService.deleteLocationAll(groupId);
    }
    // ====================== dashboards Controller ======================

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userId, Long groupId, Long locationId) {
        // 조회하려는 user가 해당 그룹의 멤버인지 검증
        groupMemberService.validateGroupMembers(groupId, userId);

        // 조회하려는 dashboard가 해당 그룹의 location에 속해있는지 검증
        locationService.getLocationByGroupId(locationId, groupId);

        return dashboardService.getDashboard(locationId);
    }

    private void validationIsAdmin(GroupMember groupMember) {
        if (groupMember.isMember()) {
            throw NoPermissionException.forAdmin(groupMember.getGroupMemberId());
        }
    }

    // ====================== widgets Controller ======================


}
