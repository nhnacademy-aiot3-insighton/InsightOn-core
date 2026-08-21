package com.insighton.core.domain.location.service.impl;

import com.insighton.core.domain.groups.entity.Group;
import com.insighton.core.domain.location.dto.request.LocationCreateRequest;
import com.insighton.core.domain.location.dto.request.LocationUpdateRequest;
import com.insighton.core.domain.location.dto.response.LocationListResponse;
import com.insighton.core.domain.location.dto.response.LocationResponse;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.exception.LocationAlreadyException;
import com.insighton.core.domain.location.exception.LocationNotFoundException;
import com.insighton.core.domain.location.repository.LocationRepository;
import com.insighton.core.domain.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {
    private final LocationRepository locationRepository;


    @Override
    @Transactional
    public Location createLocation(Group group, LocationCreateRequest request) {
        // 어떤 검증이 필요할까...
        if (locationRepository.existsByGroupGroupIdAndLocationName(group.getGroupId(), request.locationName())) {
            throw new LocationAlreadyException(request.locationName());
        }

        Location newLocation = Location.builder()
                .group(group)
                .locationName(request.locationName())
                .autoControlMode(request.autoControlMode())
                .build();

        try {
            Location savedLocation = locationRepository.saveAndFlush(newLocation);
            log.info("위치(Location) 생성 완료 - locationId: {}, locationName: {}, groupId: {}", savedLocation.getLocationId(), savedLocation.getLocationName(), group.getGroupId());
            return savedLocation;
        } catch (DataIntegrityViolationException e) {
            throw new LocationAlreadyException(request.locationName());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public List<LocationListResponse> getLocationList(Long groupId) {

        return locationRepository.findAllByGroupGroupId(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocation(Long locationId, Long groupId) {
        // 어떤 걸 검증해야하나... 흠냐
        Location location = getLocationByGroupId(locationId, groupId);

        return LocationResponse.builder()
                .locationId(locationId)
                .groupId(groupId)
                .locationName(location.getLocationName())
                .createdAt(location.getCreatedAt())
                .autoControlMode(location.getAutoControlMode())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocationAI(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(locationId));

        return LocationResponse.builder()
                .locationId(locationId)
                .groupId(location.getGroup().getGroupId())
                .locationName(location.getLocationName())
                .createdAt(location.getCreatedAt())
                .autoControlMode(location.getAutoControlMode())
                .build();
    }

    @Override
    @Transactional
    public void toggleAutoControlMode(Long locationId, Long groupId) {
        Location location = getLocationByGroupId(locationId, groupId);
        location.toggleAutoControlMode();
        log.info("위치 자동제어 모드 변경 완료 - locationId: {}, autoControlMode: {}", locationId, location.getAutoControlMode());
    }

    @Override
    @Transactional
    public void updateName(Long locationId, Long groupId, LocationUpdateRequest request) {
        Location location = getLocationByGroupId(locationId, groupId);

        if (location.getLocationName().equals(request.newLocationName())) {
            return;
        }

        if (locationRepository.existsByGroupGroupIdAndLocationName(groupId, request.newLocationName())) {
            throw new LocationAlreadyException(request.newLocationName());
        }

        location.updateName(request.newLocationName());
        log.info("위치 이름 수정 완료 - locationId: {}, newName: {}", locationId, request.newLocationName());
    }

    @Override
    @Transactional
    public void deleteLocation(Long targetLocationId, Long groupId) {
        Location location = getLocationByGroupId(targetLocationId, groupId);

        locationRepository.delete(location);
        log.info("위치 삭제 완료 - locationId: {}, groupId: {}", targetLocationId, groupId);
    }

    @Override
    @Transactional
    public void deleteLocationAll(Long groupId) {

        locationRepository.deleteAllByGroupGroupId(groupId);
        log.info("그룹 모든 위치 삭제 완료 - groupId: {}", groupId);
    }

    @Override
//    @Transactional(readOnly = true)
    public Location getLocationByGroupId(Long locationId, Long groupId) {
        return locationRepository.findByLocationIdAndGroupGroupId(locationId, groupId)
                .orElseThrow(() -> LocationNotFoundException.notFoundLocationByLocationId(locationId));
    }

    @Override
    @Transactional
    public List<Location> getLocationListByGroupId(Long groupId) {
        return locationRepository.findByGroupGroupId(groupId);
    }
}
