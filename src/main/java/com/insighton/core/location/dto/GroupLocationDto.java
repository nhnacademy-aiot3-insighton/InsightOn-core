package com.insighton.core.location.dto;

import java.time.LocalDateTime;

public record GroupLocationDto(
        Long groupId,
        LocationGridDto locationGridDto,
        LocalDateTime updatedAt
) {
}
