package com.insighton.core.service.groupmember;


import com.insighton.core.dto.groupmember.request.GroupMemberJoinRequest;

public interface GroupMembersService {

    /**
     * 그룹 가입
     * @param request 그룹 가입 요청 DTO
     */
    void joinGroupByToken(GroupMemberJoinRequest request);


}
