package com.insighton.core.location.service.impl;

import com.insighton.core.groups.entity.Groups;
import com.insighton.core.location.dto.request.LocationsCreateRequest;
import com.insighton.core.location.dto.response.LocationsListResponse;
import com.insighton.core.location.dto.response.LocationsResponse;
import com.insighton.core.location.entity.Locations;
import com.insighton.core.location.exception.LocationAlreadyException;
import com.insighton.core.location.exception.LocationNotFoundException;
import com.insighton.core.location.repository.LocationsRepository;
import com.insighton.core.location.service.LocationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationsServiceImpl implements LocationsService {
    private final LocationsRepository locationRepository;


    @Override
    @Transactional
    public void createLocation(Groups groups, LocationsCreateRequest request) {
        // 어떤 검증이 필요할까...
        if (locationRepository.existsByGroups_GroupIdAndLocationName(groups.getGroupId(), request.locationName())) {
            throw new LocationAlreadyException(request.locationName());
        }

        Locations newLocation = Locations.builder()
                .groups(groups)
                .locationName(request.locationName())
                .autoControlMode(request.autoControlMode())
                .build();

        locationRepository.save(newLocation);
    }


    @Override
    @Transactional(readOnly = true)
    public List<LocationsListResponse> getLocationList(Long groupId) {

        return locationRepository.findAllByGroups_GroupId(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public LocationsResponse getLocation(Long locationId, Long groupId) {
        // 어떤 걸 검증해야하나... 흠냐
        Locations location = locationRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)
                .orElseThrow(() -> new LocationNotFoundException(locationId));

        return LocationsResponse.builder()
                .groupId(groupId)
                .locationName(location.getLocationName())
                .createdAt(location.getCreatedAt())
                .autoControlMode(location.getAutoControlMode())
                .build();
    }


    @Override
    @Transactional
    public void toggleAutoControlMode(Long locationId, Long groupId) {
        Locations location = locationRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)
                .orElseThrow(() -> new LocationNotFoundException(locationId));

        location.toggleAutoControlMode();
    }

    @Override
    @Transactional
    public void deleteLocation(Long targetLocationId, Long groupId) {
        Locations location = locationRepository.findByLocationIdAndGroups_GroupId(targetLocationId, groupId)
                .orElseThrow(() -> new LocationNotFoundException(targetLocationId));

        locationRepository.delete(location);
    }


}
