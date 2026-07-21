package com.insighton.core.location.repository;

import com.insighton.core.location.dto.GroupLocationDto;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.springframework.stereotype.Repository;

@Repository
@Getter
public class InMemoryGroupLocationRepository implements GroupLocationRepository {

    private final Map<Long, GroupLocationDto> groupLocationDtoMap = new ConcurrentHashMap<>();

    @Override
    public void save(GroupLocationDto dto) {
        groupLocationDtoMap.put(dto.groupId(), dto);
    }

    @Override
    public Optional<GroupLocationDto> findByGroupId(Long groupId) {
        return Optional.ofNullable(groupLocationDtoMap.get(groupId));
    }

    @Override
    public void deleteByGroupId(Long groupId) {
        groupLocationDtoMap.remove(groupId);
    }

    @Override
    public void clear() {
        groupLocationDtoMap.clear();
    }
}
