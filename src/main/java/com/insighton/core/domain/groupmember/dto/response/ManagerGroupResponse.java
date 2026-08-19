package com.insighton.core.domain.groupmember.dto.response;

import lombok.Builder;

@Builder
public record ManagerGroupResponse(
        boolean exists,
        String groupName
) {
}
