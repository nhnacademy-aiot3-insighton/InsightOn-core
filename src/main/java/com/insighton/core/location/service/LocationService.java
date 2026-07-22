package com.insighton.core.location.service;

import com.insighton.core.exception.LocationNotFoundException;
import com.insighton.core.location.dto.GroupLocationDto;
import com.insighton.core.location.dto.LocationGridDto;
import com.insighton.core.location.dto.LocationRequestDto;
import com.insighton.core.location.dto.LocationResponseDto;
import com.insighton.core.location.registry.LocationRegistry;
import com.insighton.core.location.repository.GroupLocationRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRegistry locationRegistry;
    private final GroupLocationRepository groupLocationRepository;

    public List<String> getSortedStates() {
        List<String> states = locationRegistry.findAllStates();
        Collections.sort(states); // 오름차순 정렬
        return states;
    }

    public List<String> getSortedCities(String state) {
        List<String> cities = locationRegistry.findCitiesByState(state);
        Collections.sort(cities);
        return cities;
    }

    public LocationResponseDto selectGroupLocation(LocationRequestDto requestDto) {
        Optional<LocationGridDto> optionalLocationGridDto = locationRegistry.findGridCoordinate(
                requestDto.step1(), requestDto.step2());
        if (optionalLocationGridDto.isEmpty()) {
            throw new LocationNotFoundException("존재하지 않는 행정구역 좌표 정보입니다.");
        }

        LocationGridDto gridDto = optionalLocationGridDto.get();

        GroupLocationDto groupLocationDto = new GroupLocationDto(
                requestDto.groupId(),
                gridDto,
                LocalDateTime.now()
        );

        groupLocationRepository.save(groupLocationDto);

        return new LocationResponseDto(
                groupLocationDto.groupId(),
                groupLocationDto.locationGridDto()
        );
    }
}
