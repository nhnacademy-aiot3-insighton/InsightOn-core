package com.insighton.core.groupmember.dto.response;

import lombok.Builder;

/**
 * auth에서 user정보를 넘겨받기 위한 DTO
 *
 * @param userId          조회할 user ID
 * @param userName        받을 user name
 * @param userPhoneNumber 받을 user의 phone number
 */
@Builder
public record AuthUserResponse(
        Long userId,
        String userName,
        String userPhoneNumber
) {
}
