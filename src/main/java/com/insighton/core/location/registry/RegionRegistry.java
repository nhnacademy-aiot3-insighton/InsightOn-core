package com.insighton.core.location.registry;

import com.insighton.core.location.dto.RegionGridDto;
import java.util.List;
import java.util.Optional;

public interface RegionRegistry {
    void save(RegionGridDto dto);

    List<String> findAllStates();

    List<String> findCitiesByState(String state);

    Optional<RegionGridDto> findGridCoordinate(String state, String city);

    void clear();
}
