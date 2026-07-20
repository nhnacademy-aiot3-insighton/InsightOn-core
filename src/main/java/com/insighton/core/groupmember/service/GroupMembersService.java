package com.insighton.core.groupmember.service;


import com.insighton.core.groupmember.dto.request.GroupMembersJoinRequest;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groups.entity.Groups;

public interface GroupMembersService {

    /**
     * 그룹 가입
     * @param request 그룹 가입 요청 DTO
     */
    void joinGroupByToken(GroupMembersJoinRequest request);

    /**
     * 그룹 생성자. 바로 SUPER_MANAGER 권한 가지는 GroupMember 생성
     * @param groups 생성된 group의 정보
     * @param userId login한 user의 id
     */
    void createGroupMember(Groups groups, Long userId);

    /**
     * 특정 그룹에 속한 멤버인지 검증하고 멤버 객체 반환
     */
    GroupMembers validateGroupMembers(Long groupId, Long userId);

    /**
     * member 권한 변경
     * @param groupId 그들이 속한 group ID
     * @param targetUserId 권한을 변경 타겟의 user의 ID
     * @param adminId 변경 권한을 가진 user의 ID
     */
    void toggleManagerRole(Long groupId, Long targetUserId, Long adminId);

    /**
     * 시스템에 존재하는 올바른 유저인지 확인
     */
    void validateUserExists(Long userId);

    /**
     * 그룹 멤버의 권환 확인용
     */
    boolean isGroupAdmin(Long groupId, Long userId);

}
