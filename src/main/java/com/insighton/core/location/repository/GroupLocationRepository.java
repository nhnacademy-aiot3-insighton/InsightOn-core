package com.insighton.core.location.repository;

import com.insighton.core.location.dto.GroupLocationDto;
import java.util.Optional;

public interface GroupLocationRepository {
    void save(GroupLocationDto dto);

    Optional<GroupLocationDto> findByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);

    void clear();
}
