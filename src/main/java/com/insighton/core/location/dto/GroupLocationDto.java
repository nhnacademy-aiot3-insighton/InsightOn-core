package com.insighton.core.location.dto;

import java.time.OffsetDateTime;

public record GroupLocationDto(
        Long groupId,
        LocationGridDto locationGridDto,
        OffsetDateTime updatedAt
) {
}
