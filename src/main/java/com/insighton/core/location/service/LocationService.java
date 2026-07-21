package com.insighton.core.location.service;

import com.insighton.core.exception.LocationNotFoundException;
import com.insighton.core.location.dto.LocationGridDto;
import com.insighton.core.location.repository.InMemoryLocationRegistry;
import com.insighton.core.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final InMemoryLocationRegistry inMemoryLocationRegistry;
    private final LocationRepository locationRepository;

    public List<String> getSortedStates() {
        List<String> states = locationRepository.findAllStates();
        Collections.sort(states); // 오름차순 정렬
        return states;
    }

    public List<String> getSortedCities(String state) {
        List<String> cities = locationRepository.findCitiesByState(state);
        Collections.sort(cities);
        return cities;
    }

    public LocationGridDto getGridCoordinate(String state, String city) {
        Optional<LocationGridDto> optionalLocationGridDto = locationRepository.findGridCoordinate(state, city);
        if (optionalLocationGridDto.isEmpty()) {
            throw new LocationNotFoundException("존재하지 않는 행정구역 좌표 정보입니다.");
        }
        return optionalLocationGridDto.get();
    }
}
