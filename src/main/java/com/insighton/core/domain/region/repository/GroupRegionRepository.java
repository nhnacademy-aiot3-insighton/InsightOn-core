package com.insighton.core.domain.region.repository;

import com.insighton.core.domain.region.dto.GroupRegionDto;
import java.util.List;
import java.util.Optional;

public interface GroupRegionRepository {
    void save(GroupRegionDto dto);

    Optional<GroupRegionDto> findByGroupId(Long groupId);

    List<GroupRegionDto> findAll();

    void deleteByGroupId(Long groupId);

    void clear();
}
