package com.insighton.core.location.dto;

import jakarta.validation.constraints.NotBlank;

public record RegionRequestDto(
        @NotBlank Long groupId,
        @NotBlank String step1,
        @NotBlank String step2
) {
}
