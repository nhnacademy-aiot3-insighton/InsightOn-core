package com.insighton.core.domain.region.service;

import com.insighton.core.domain.region.dto.GroupRegionDto;
import com.insighton.core.domain.region.dto.RegionGridDto;
import com.insighton.core.domain.region.exception.RegionNotFoundException;
import com.insighton.core.domain.region.registry.RegionRegistry;
import com.insighton.core.domain.region.repository.GroupRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    public RegionGridDto validateRegion(String state, String city) {
        return regionRegistry.findGridCoordinate(state, city)
                .orElseThrow(() -> new RegionNotFoundException("존재하지 않는 행정구역입니다: " + state + " " + city));
    }

    public GroupRegionDto cacheGroupRegion(Long groupId, String groupRegion) {
        String[] parts = groupRegion.split(" ", 2);
        if (parts.length != 2) {
            throw new RegionNotFoundException("올바르지 않은 지역 형식입니다: " + groupRegion);
        }
        RegionGridDto gridDto = validateRegion(parts[0], parts[1]);
        GroupRegionDto groupRegionDto = new GroupRegionDto(groupId, gridDto, OffsetDateTime.now(ZoneId.systemDefault()));
        groupRegionRepository.save(groupRegionDto);
        return groupRegionDto;
    }

    public Optional<GroupRegionDto> findByGroupRegion(Long groupId) {
        return groupRegionRepository.findByGroupId(groupId);
    }
}