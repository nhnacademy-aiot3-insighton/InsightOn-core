package com.insighton.core.domain.groupmember.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;

/**
 * group 가입 요청
 *
 * @param inviteToken group 초대 토큰
 * @param userId      login한 user의 ID
 */
@Builder
public record GroupMemberJoinRequest(
        @NotBlank
        @Length(max = 12)
        String inviteToken,
        @NotNull Long userId
) {
}
