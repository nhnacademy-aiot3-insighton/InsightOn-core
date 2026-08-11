package com.insighton.core.usecase;

import com.insighton.core.common.annotation.UseCase;
import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.domain.region.dto.GroupRegionDto;
import com.insighton.core.domain.region.service.RegionService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GroupRegionResolutionUseCase {

    private final RegionService regionService;
    private final GroupService groupService;

    public GroupRegionDto resolve(Long groupId) {
        return regionService.findByGroupRegion(groupId)
                .orElseGet(() -> {
                    Group group = groupService.groupFindById(groupId);
                    return regionService.cacheGroupRegion(groupId, group.getGroupRegion());
                });
    }
}
