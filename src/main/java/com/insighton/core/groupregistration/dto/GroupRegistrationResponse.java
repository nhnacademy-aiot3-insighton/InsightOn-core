package com.insighton.core.groupregistration.dto;

import com.insighton.core.groupregistration.entity.GroupRegistrationStatus;

import java.time.OffsetDateTime;

public record GroupRegistrationResponse(Long groupRegistrationId,
                                        Long requesterId,
                                        String groupName,
                                        String description,
                                        String groupRegion,
                                        GroupRegistrationStatus status,
                                        Long approverId,
                                        OffsetDateTime createdAt,
                                        OffsetDateTime approvedAt) { }
