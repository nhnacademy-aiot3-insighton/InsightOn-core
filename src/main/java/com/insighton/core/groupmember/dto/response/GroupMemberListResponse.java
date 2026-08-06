package com.insighton.core.groupmember.dto.response;

import com.insighton.core.groupmember.entity.GroupMember;
import lombok.Builder;

/**
 * group에 속해있는 member List
 *
 * @param userId    user의 ID
 * @param groupRole user의 group 내 권한
 */
@Builder
public record GroupMemberListResponse(
        Long userId,
        GroupMember.GroupRole groupRole) {
}
