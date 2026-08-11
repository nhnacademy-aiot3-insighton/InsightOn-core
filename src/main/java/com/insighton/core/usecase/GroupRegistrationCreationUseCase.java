package com.insighton.core.usecase;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groupregistration.dto.CreateGroupRegistrationRequest;
import com.insighton.core.domain.groupregistration.dto.GroupRegistrationResponse;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.domain.region.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GroupRegistrationCreationUseCase {

    private final GroupRegistrationService groupRegistrationService;
    private final RegionService regionService;

    @Transactional
    public GroupRegistrationResponse createRequest(Long requesterId, CreateGroupRegistrationRequest request) {
        regionService.validateRegion(request.state(), request.city());

        String groupRegion = "%s,%s".formatted(request.state(), request.city());
        return groupRegistrationService.createRequest(requesterId, request.groupName(), request.description(), groupRegion);
    }
}
