package com.insighton.core.location.dto;

public record LocationRequestDto(
        Long groupId,
        String step1,
        String step2
) {
}
