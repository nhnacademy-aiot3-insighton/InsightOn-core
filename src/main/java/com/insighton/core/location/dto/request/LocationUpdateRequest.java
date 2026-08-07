package com.insighton.core.location.dto.request;

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
