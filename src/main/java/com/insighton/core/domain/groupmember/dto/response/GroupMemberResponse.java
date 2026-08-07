package com.insighton.core.domain.groupmember.dto.response;

import com.insighton.core.domain.groupmember.entity.GroupMember;
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
public record GroupMemberResponse(
        Long userId,
        Long groupId,
        GroupMember.GroupRole groupRole,
        String userName,
        String userPhoneNumber,
        OffsetDateTime joinedAt
) {
}
