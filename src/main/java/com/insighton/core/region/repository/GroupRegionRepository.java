package com.insighton.core.region.repository;

import com.insighton.core.region.dto.GroupRegionDto;
import java.util.Optional;

public interface GroupRegionRepository {
    void save(GroupRegionDto dto);

    Optional<GroupRegionDto> findByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);

    void clear();
}
