package com.insighton.core.domain.location.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 *
 * @param newLocationName
 */
@Builder
public record LocationUpdateRequest(
        @NotBlank String newLocationName
) {
}
