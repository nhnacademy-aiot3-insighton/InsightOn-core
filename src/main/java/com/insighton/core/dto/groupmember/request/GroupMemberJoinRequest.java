package com.insighton.core.dto.groupmember.request;

import lombok.Builder;

/**
 * group 가입 요청
 * @param inviteToken group 초대 토큰
 * @param userId login한 user의 ID
 */
@Builder
public record GroupMemberJoinRequest(
        String inviteToken,
        Long userId
) {
}
