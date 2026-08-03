package com.insighton.core.location.service.impl;

import com.insighton.core.groups.entity.Group;
import com.insighton.core.location.dto.request.LocationCreateRequest;
import com.insighton.core.location.dto.request.LocationUpdateRequest;
import com.insighton.core.location.dto.response.LocationListResponse;
import com.insighton.core.location.dto.response.LocationResponse;
import com.insighton.core.location.entity.Location;
import com.insighton.core.location.exception.LocationAlreadyException;
import com.insighton.core.location.exception.LocationNotFoundException;
import com.insighton.core.location.repository.LocationRepository;
import com.insighton.core.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {
    private final LocationRepository locationRepository;


    @Override
    @Transactional
    public Location createLocation(Group group, LocationCreateRequest request) {
        // 어떤 검증이 필요할까...
        if (locationRepository.existsByGroups_GroupIdAndLocationName(group.getGroupId(), request.locationName())) {
            throw new LocationAlreadyException(request.locationName());
        }

        Location newLocation = Location.builder()
                .groups(group)
                .locationName(request.locationName())
                .autoControlMode(request.autoControlMode())
                .build();

        return locationRepository.save(newLocation);
    }


    @Override
    @Transactional(readOnly = true)
    public List<LocationListResponse> getLocationList(Long groupId) {

        return locationRepository.findAllByGroups_GroupId(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocation(Long locationId, Long groupId) {
        // 어떤 걸 검증해야하나... 흠냐
        Location location = locationRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)
                .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(locationId));

        return LocationResponse.builder()
                .locationId(locationId)
                .groupId(groupId)
                .locationName(location.getLocationName())
                .createdAt(location.getCreatedAt())
                .autoControlMode(location.getAutoControlMode())
                .build();
    }


    @Override
    @Transactional
    public void toggleAutoControlMode(Long locationId, Long groupId) {
        Location location = locationRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)
                .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(locationId));

        location.toggleAutoControlMode();
    }

    @Override
    @Transactional
    public void updateName(Long locationId, Long groupId, LocationUpdateRequest request) {
        Location location = locationRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)
                .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(locationId));

        location.updateName(request.newLocationName());
    }

    @Override
    @Transactional
    public void deleteLocation(Long targetLocationId, Long groupId) {
        Location location = locationRepository.findByLocationIdAndGroups_GroupId(targetLocationId, groupId)
                .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(targetLocationId));

        locationRepository.delete(location);
    }

    @Override
    @Transactional
    public void deleteLocationAll(Long groupId) {

        locationRepository.deleteAllByGroups_GroupId(groupId);
    }

    @Override
    @Transactional
    public Location getLocationByGroupId(Long groupId) {
        return locationRepository.findByGroups_GroupId(groupId)
                .orElseThrow(() -> LocationNotFoundException.notFoundLocationByGroupId(groupId));
    }
}
