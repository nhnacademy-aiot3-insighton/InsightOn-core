package com.insighton.core.region.dto;

import java.time.OffsetDateTime;

public record GroupRegionDto(
        Long groupId,
        RegionGridDto regionGridDto,
        OffsetDateTime updatedAt
) {
}
