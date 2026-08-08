package com.insighton.core.domain.groupmember.dto.response;

import lombok.Builder;

/**
 * auth에서 user정보를 넘겨받기 위한 DTO
 *
 * @param userId          조회할 user ID
 * @param userName        받을 user name
 * @param userPhoneNumber 받을 user의 phone number
 * @param userStatus      받을 user의 계정의 상태
 */
@Builder
public record AuthUserResponse(
        Long userId,
        String userName,
        String userPhoneNumber,
        String userStatus
) {
}
