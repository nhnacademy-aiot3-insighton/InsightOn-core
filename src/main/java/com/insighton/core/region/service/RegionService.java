package com.insighton.core.region.service;

import com.insighton.core.exception.LocationNotFoundException;
import com.insighton.core.region.dto.GroupRegionDto;
import com.insighton.core.region.dto.RegionGridDto;
import com.insighton.core.region.dto.RegionRequestDto;
import com.insighton.core.region.dto.RegionResponseDto;
import com.insighton.core.region.registry.RegionRegistry;
import com.insighton.core.region.repository.GroupRegionRepository;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRegistry regionRegistry;
    private final GroupRegionRepository groupRegionRepository;

    public List<String> getSortedStates() {
        List<String> states = regionRegistry.findAllStates();
        Collections.sort(states); // 오름차순 정렬
        return states;
    }

    public List<String> getSortedCities(String state) {
        List<String> cities = regionRegistry.findCitiesByState(state);
        Collections.sort(cities);
        return cities;
    }

    public RegionResponseDto selectGroupLocation(RegionRequestDto requestDto) {
        Optional<RegionGridDto> optionalLocationGridDto = regionRegistry.findGridCoordinate(
                requestDto.step1(), requestDto.step2());
        if (optionalLocationGridDto.isEmpty()) {
            throw new LocationNotFoundException("존재하지 않는 행정구역 좌표 정보입니다.");
        }

        RegionGridDto gridDto = optionalLocationGridDto.get();

        GroupRegionDto groupLocationDto = new GroupRegionDto(
                requestDto.groupId(),
                gridDto,
                OffsetDateTime.now()
        );

        groupRegionRepository.save(groupLocationDto);

        return new RegionResponseDto(
                groupLocationDto.groupId(),
                groupLocationDto.regionGridDto()
        );
    }
}
