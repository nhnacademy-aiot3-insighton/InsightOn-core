package com.insighton.core.groupmember.dto.response;

import com.insighton.core.groupmember.entity.GroupMembers;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * group member 상세 정보
 *
 * @param userId          user의 ID
 * @param groupId         user가 속한 group의 ID
 * @param groupRole       user의 group 내 권한
 * @param userName        user의 이름
 * @param userPhoneNumber user의 phoneNumber
 * @param joinedAt        user의 group 가입 날짜
 */
@Builder
public record GroupMembersResponse(
        Long userId,
        Long groupId,
        GroupMembers.GroupRole groupRole,
        String userName,
        String userPhoneNumber,
        OffsetDateTime joinedAt
) {
}
