package com.insighton.core.domain.groupmember.dto.response;

import lombok.Builder;

@Builder
public record UserGroupResponse(
        boolean exists,
        String groupName
) {
}
