package com.insighton.core.domain.groups.event;

public record GroupRegionUpdateEvent(
        Long groupId,
        String groupRegion
) {
}
