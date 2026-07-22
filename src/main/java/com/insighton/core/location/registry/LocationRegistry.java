package com.insighton.core.location.registry;

import com.insighton.core.location.dto.LocationGridDto;
import java.util.List;
import java.util.Optional;

public interface LocationRegistry {
    void save(LocationGridDto dto);

    List<String> findAllStates();

    List<String> findCitiesByState(String state);

    Optional<LocationGridDto> findGridCoordinate(String state, String city);

    void clear();
}
