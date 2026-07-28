package com.insighton.core.location.dto;

import java.time.OffsetDateTime;

public record GroupRegionDto(
        Long groupId,
        RegionGridDto regionGridDto,
        OffsetDateTime updatedAt
) {
}
