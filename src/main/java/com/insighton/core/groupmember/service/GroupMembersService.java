package com.insighton.core.groupmember.service;


import com.insighton.core.groupmember.dto.request.GroupMembersJoinRequest;
import com.insighton.core.groupmember.entity.GroupMembers;

public interface GroupMembersService {

    /**
     * 그룹 가입
     * @param request 그룹 가입 요청 DTO
     */
    void joinGroupByToken(GroupMembersJoinRequest request);

    GroupMembers validateGroupMembers(Long groupId, Long userId);

    void validateUserExists(Long userId);

}
