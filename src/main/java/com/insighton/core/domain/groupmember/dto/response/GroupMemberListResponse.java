package com.insighton.core.domain.groupmember.dto.response;

import com.insighton.core.domain.groupmember.entity.GroupMember;
import lombok.Builder;

/**
 * group에 속해있는 member List
 *
 * @param groupMemberId groupMember의 ID
 * @param userId        user의 ID
 * @param groupRole     user의 group 내 권한
 */
@Builder
public record GroupMemberListResponse(
        Long groupMemberId,
        Long userId,
        GroupMember.GroupRole groupRole) {
}
