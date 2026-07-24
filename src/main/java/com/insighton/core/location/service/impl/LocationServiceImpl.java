package com.insighton.core.location.service.impl;

import com.insighton.core.groups.entity.Groups;
import com.insighton.core.location.dto.request.LocationCreateRequest;
import com.insighton.core.location.dto.response.LocationListResponse;
import com.insighton.core.location.dto.response.LocationResponse;
import com.insighton.core.location.entity.Location;
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

    // groupsmanagementUsecase에서 group이 존재하는지 확인
    @Override
    @Transactional
    public void createLocation(Groups groups, LocationCreateRequest request) {
        // 어떤 검증이 필요할까...

        Location newLocation = Location.builder()
                .groups(groups)
                .locationName(request.locationName())
                .autoControlMode(request.autoControlMode())
                .build();

        locationRepository.save(newLocation);
    }


    @Override
    @Transactional(readOnly = true)
    public List<LocationListResponse> getLocationList(Long groupId) {

        return locationRepository.findAllByGroups_GroupId(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocation(Long groupId, Long locationId) {
        // 어떤 걸 검증해야하나... 흠냐
        return locationRepository.findByLocationIdAndGroups_GroupId(locationId, groupId)
                .orElseThrow(() -> new LocationNotFoundException(locationId));
    }


    @Override
    @Transactional
    public void toggleAutoControlMode(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new LocationNotFoundException(locationId));

        location.toggleAutoControlMode();
    }

    @Override
    @Transactional
    public void deleteLocation(Long targetLocationId) {

        Location location = locationRepository.findById(targetLocationId)
                .orElseThrow(() -> new LocationNotFoundException(targetLocationId));

        locationRepository.delete(location);
    }


}
