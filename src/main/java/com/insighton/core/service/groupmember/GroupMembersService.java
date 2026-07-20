package com.insighton.core.service.groupmember;


import com.insighton.core.dto.groupmember.request.GroupMembersJoinRequest;
import com.insighton.core.entity.groupmember.GroupMembers;

public interface GroupMembersService {

    /**
     * 그룹 가입
     * @param request 그룹 가입 요청 DTO
     */
    void joinGroupByToken(GroupMembersJoinRequest request);

    GroupMembers validateGroupMembers(Long groupId, Long userId);

    void validateUserExists(Long userId);

}
