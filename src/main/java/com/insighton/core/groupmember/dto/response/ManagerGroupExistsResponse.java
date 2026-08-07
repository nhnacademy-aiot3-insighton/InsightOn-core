package com.insighton.core.groupmember.dto.response;

import lombok.Builder;

@Builder
public record ManagerGroupExistsResponse(
        boolean exists
) {
}
