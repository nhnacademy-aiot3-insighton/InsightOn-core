package com.insighton.core.groupregistration.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupRegistrationRequest(@NotBlank String groupName,
                                             String description,
                                             @NotBlank String groupRegion) {}
