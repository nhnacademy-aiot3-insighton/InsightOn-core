package com.insighton.core.domain.groupregistration.presentation;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupRegistrationRequest(@NotBlank String groupName,
                                             String description,
                                             @NotBlank String groupRegion) {}
