package com.insighton.core.domain.region.dto;

import java.time.OffsetDateTime;

public record GroupRegionDto(
        Long groupId,
        RegionGridDto regionGridDto,
        OffsetDateTime updatedAt
) {
}
