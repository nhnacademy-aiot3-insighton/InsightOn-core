package com.insighton.core.groups.service;

import com.insighton.core.gateway.service.GatewayService;
import com.insighton.core.groupmember.dto.request.GroupMembersJoinRequest;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.client.EngineClient;
import com.insighton.core.groups.dto.request.GroupsRequest;
import com.insighton.core.groups.dto.response.GroupsResponse;
import com.insighton.core.groups.entity.Groups;
import com.insighton.core.groups.exception.NoPermissionException;
import com.insighton.core.groups.exception.UnAuthorizedAccessException;
import com.insighton.core.location.dto.request.LocationsCreateRequest;
import com.insighton.core.location.dto.response.LocationsListResponse;
import com.insighton.core.location.dto.response.LocationsResponse;
import com.insighton.core.location.service.LocationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupManagementUseCase {
    private final GroupsService groupService;
    private final GroupMembersService groupMembersService;
    private final LocationsService locationService;
    private final GatewayService gatewayService;
    private final EngineClient engineClient;

    // ====================== Group Controller ======================

    /**
     * 그룹 가입
     *
     * @param request 그룹 가입 요청 DTO
     */
    @Transactional
    public void joinGroupByToken(GroupMembersJoinRequest request) {
        // inviteToken으로 대상 그룹이 존재하는지 확인 및 조회
        Groups groups = groupService.validateGroupByInviteToken(request.inviteToken());

        groupMembersService.joinGroupByToken(groups, request);
    }

    /**
     * 그룹 생성 시 SUPER_MANAGER로 등록되는
     *
     * @param groupsCreateRequest 그룹 생성 요청 정보
     * @param userId              그룹을 생성하는 user의 ID
     */
    @Transactional
    public void createGroup(GroupsRequest groupsCreateRequest, Long userId) {
        Groups groups = groupService.createGroup(groupsCreateRequest);

        groupMembersService.createGroupMember(groups, userId);
    }


    /**
     * 그룹 수정
     *
     * @param request Group 수정 요청 정보
     * @param userId  login한 user의 ID
     * @param groupId 수정하려는 group의 ID
     */
    @Transactional
    public void updateGroup(GroupsRequest request, Long userId, Long groupId) {
        if (groupMembersService.isGroupAdmin(groupId, userId)) {
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
    public GroupsResponse getGroupPreview(String inviteToken, Long userId, Long groupId) {
        // 유저가 존재하는지 검증(다른 그룹에 가입이 안 되어있어야함 )
        groupMembersService.validateUserNotInAnyGroup(userId);

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
    public GroupsResponse getMyGroup(Long userId, Long groupId) {
        // 이건 group안에 user가 속해있는지 같이 보기 위한 메서드임...
        GroupMembers members = groupMembersService.validateGroupMembers(groupId, userId);

        // 관리자나 그룹 생성자일 때는
        if (members.isManager() || members.isSuperManager()) {
            return GroupsResponse.ofAdmin(members.getGroups());
        }

        // 대상 그룹 조회 (없을 시 exception 던지기)
        return GroupsResponse.ofPublic(members.getGroups());
    }


    /**
     * 토큰 재발급
     *
     * @param groupId 재발급 하려는 group의 ID
     * @param userId  재발급 하려는 user의 ID
     */
    @Transactional
    public void newInviteToken(Long userId, Long groupId) {
        GroupMembers groupMembers = groupMembersService.validateGroupMembers(groupId, userId);

        if (groupMembers.isMember()) {
            throw NoPermissionException.forAdmin(groupMembers.getGroupMemberId());
        }
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
        GroupMembers groupMembers = groupMembersService.validateGroupMembers(groupId, userId);

        if (!groupMembers.isSuperManager()) {
            throw NoPermissionException.forAdmin(groupMembers.getGroupMemberId());
        }

        engineClient.deleteEnginesByGroupId(groupId);
        // gateway 삭제하기 전부.

        groupMembersService.deleteGroupMemberAll(userId, groupId);

        deleteLocationAll(groupId);

        groupService.deleteGroup(groupId);
    }

    // ====================== Locations Controller ======================

    /**
     * location 생성
     *
     * @param userId  location을 생성하려는 user의 ID
     * @param groupId location을 만드려는 group ID
     * @param request location 생성 request
     */
    @Transactional
    public void createLocation(Long userId, Long groupId, LocationsCreateRequest request) {
        // 그룹이 존재하는지 확인하고
        Groups groups = groupService.groupFindById(groupId);

        // 그룹이 존재한다면 그 그룹 안에 location을 만드려는 사람이 존재하는지 확인하고
        GroupMembers groupMembers = groupMembersService.validateGroupMembers(groupId, userId);

        // member가 member 권한일 때는 에러를 던지고
        if (groupMembers.isMember()) {
            throw NoPermissionException.forAdmin(groupMembers.getGroupMemberId());
        }

        // 만들기
        locationService.createLocation(groups, request);
    }

    /**
     * location list 조회
     *
     * @param userId  list를 조회하는 user의 ID
     * @param groupId location들이 속해있는 group의 ID
     * @return location List 반환
     */
    @Transactional(readOnly = true)
    public List<LocationsListResponse> getLocationList(Long userId, Long groupId) {
        // 그룹에 user가 존재하는지 확인
        groupMembersService.validateGroupMembers(groupId, userId);

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
    public LocationsResponse getLocation(Long userId, Long groupId, Long locationId) {
        // 그룹에 user가 존재하는지 확인
        groupMembersService.validateGroupMembers(groupId, userId);

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

        GroupMembers groupMembers = groupMembersService.validateGroupMembers(groupId, userId);

        // member가 member 권한일 때는 에러를 던지고
        if (groupMembers.isMember()) {
            throw NoPermissionException.forAdmin(groupMembers.getGroupMemberId());
        }

        locationService.toggleAutoControlMode(locationId, groupId);
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
        GroupMembers groupMembers = groupMembersService.validateGroupMembers(groupId, userId);

        // member가 member 권한일 때는 에러를 던지고
        if (groupMembers.isMember()) {
            throw NoPermissionException.forAdmin(groupMembers.getGroupMemberId());
        }

        // devices는 location 값만 null로 바꿔주기

        // dashboards 삭제

        locationService.deleteLocation(targetLocationId, groupId);
    }

    private void deleteLocationAll(Long groupId) {

        // locationID에 해당하는 모든? devices와 dashboards 지우는 로직 추가하기

        // devices는 location 값만 null로 바꿔주기

        // dashboards 삭제
        locationService.deleteLocationAll(groupId);
    }

}
