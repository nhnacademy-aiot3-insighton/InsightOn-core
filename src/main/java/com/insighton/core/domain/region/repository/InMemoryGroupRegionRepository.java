package com.insighton.core.domain.region.repository;

import com.insighton.core.domain.region.dto.GroupRegionDto;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.springframework.stereotype.Repository;

@Repository
@Getter
public class InMemoryGroupRegionRepository implements GroupRegionRepository {

    private final Map<Long, GroupRegionDto> groupRegionDtoMap = new ConcurrentHashMap<>();

    @Override
    public void save(GroupRegionDto dto) {
        groupRegionDtoMap.put(dto.groupId(), dto);
    }

    @Override
    public Optional<GroupRegionDto> findByGroupId(Long groupId) {
        return Optional.ofNullable(groupRegionDtoMap.get(groupId));
    }

    @Override
    public void deleteByGroupId(Long groupId) {
        groupRegionDtoMap.remove(groupId);
    }

    @Override
    public void clear() {
        groupRegionDtoMap.clear();
    }
}
