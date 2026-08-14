package com.insighton.core.domain.region.dto;

public record RegionGridDto(
        String step1, // 광역시/도
        String step2, // 시/군/구
        int gridX,
        int gridY
) {
}
