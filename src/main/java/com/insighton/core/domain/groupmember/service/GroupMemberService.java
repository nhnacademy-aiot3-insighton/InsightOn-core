package com.insighton.core.domain.groupmember.service;


import com.insighton.core.domain.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.domain.groupmember.dto.response.GroupMemberListResponse;
import com.insighton.core.domain.groupmember.dto.response.GroupMemberResponse;
import com.insighton.core.domain.groupmember.dto.response.ManagerGroupResponse;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groups.entity.Group;

import java.util.List;

public interface GroupMemberService {

    /**
     * 그룹 가입
     *
     * @param group   가입하려는 group의 정보
     * @param request 그룹 가입 요청 DTO
     */
    void joinGroupByToken(Group group, GroupMemberJoinRequest request);

    /**
     * 그룹 생성자. 바로 SUPER_MANAGER 권한 가지는 GroupMember 생성
     *
     * @param group  생성된 group의 정보
     * @param userId login한 user의 id
     */
    void createGroupMember(Group group, Long userId);


    /**
     * 관리자용 GroupMember List 조회
     *
     * @param userId  login한 user의 ID
     * @param groupId member가 속한 group의 ID
     * @return GroupMemberList 반환!!
     */
    List<GroupMemberListResponse> getGroupMemberList(Long userId, Long groupId);


    /**
     * group member 상세 조회(관리자나 자기 자신만 member의 정보를 볼 수 잇음)
     *
     * @param userId  조회하는 userId
     * @param groupId member가 속해있는 group의 ID
     * @return groupMember의 정보
     */
    GroupMemberResponse getGroupMember(Long userId, Long groupId, Long groupMemberId);

    /**
     * 그룹 소속 확인용
     *
     * @param userId  소속 여부를 확인할 유저 ID
     * @param groupId Group ID
     * @return 멤버면 groupID
     */
    GroupMemberResponse getGroupMemberAI(Long userId, Long groupId);

    /**
     * userId만으로 소속 group의 ID를 조회 (한 유저는 최대 한 그룹에만 소속됨).
     * front가 로그인 시점에 groupId를 알아내기 위해 사용.
     *
     * @param userId 조회할 user의 ID
     * @return 소속된 groupId
     */
    Long getMyGroupId(Long userId);

    /**
     * member 권한 변경
     *
     * @param groupId             그들이 속한 group ID
     * @param targetGroupMemberId 권한을 변경 타겟의 user의 ID
     * @param adminId             변경 권한을 가진 user의 ID
     */
    void toggleManagerRole(Long groupId, Long targetGroupMemberId, Long adminId);

    /**
     * super manager 권한 양도
     *
     * @param groupId             그들이 속한 group ID
     * @param targetGroupMemberId super manager 권한을 양도 할 타겟의 group member의 ID
     * @param superManagerUserId  양도 할 권한을 가진 user의 ID
     */
    void toggleSuperManagerRole(Long groupId, Long targetGroupMemberId, Long superManagerUserId);


    /**
     * 그룹 멤버 삭제(관리자용)
     *
     * @param adminId             삭제하려는 자의 user ID
     * @param groupId             속해있는 groupID?
     * @param targetGroupMemberId 삭제당할 groupMember의 ID
     */
    void kickGroupMember(Long adminId, Long groupId, Long targetGroupMemberId);


    /**
     * 그룹 탈퇴 (하지만 supermanager의 권한을 양도하는 건 아직 없음)
     *
     * @param userId 탈퇴하려는 user ID
     */
    void leaveGroup(Long groupId, Long userId);


    /**
     * 그룹이 삭제될 때 자동으로 member도 모두 삭제
     *
     * @param adminId 관리자 ID
     * @param groupId 삭제되는 group의 ID...?
     */
    void deleteGroupMemberAll(Long adminId, Long groupId);

    /**
     * 그룹 멤버의 권환 확인용
     */
    boolean isGroupAdmin(Long groupId, Long userId);

    /**
     * 특정 그룹에 속한 멤버인지 검증하고 멤버 객체 반환
     */
    GroupMember validateGroupMembers(Long groupId, Long userId);

    /**
     * 그룹 멤버인지 검증한 뒤 MANAGER 이상 권한인지까지 확인하고, member 권한이면 NoPermissionException을 던짐.
     * 이 "멤버십 확인 + 권한 체크" 조합을 UseCase마다 private 메서드로 각자 복사해서 쓰고 있었는데,
     * private이라 그 로직만 독립적으로 단위 테스트하기 어렵고 코드도 중복돼서 여기 하나로 모음.
     */
    GroupMember validateGroupAdmin(Long groupId, Long userId);

    /**
     * 아무데도 가입이 안 되어있따.
     */
    void validateUserNotInAnyGroup(Long userId);


    /**
     * Auth에서 요청한
     *
     * @param userId 권한을 가진 member로 존재하는지 알고 싶은 user
     * @return 있으면 true, 없으면 false
     */
    ManagerGroupResponse existsManagerGroupAuth(Long userId);

}
