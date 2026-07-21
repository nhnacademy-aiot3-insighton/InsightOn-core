package com.insighton.core.location.controller;

import com.insighton.core.location.dto.LocationRequestDto;
import com.insighton.core.location.dto.LocationResponseDto;
import com.insighton.core.location.service.LocationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/states")
    public ResponseEntity<List<String>> getAllStates() {
        List<String> states = locationService.getSortedStates();
        return ResponseEntity.ok(states);
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCitiesByState(@RequestParam String state) {
        List<String> cities = locationService.getSortedCities(state);
        return ResponseEntity.ok(cities);
    }

    @GetMapping("/select")
    public ResponseEntity<LocationResponseDto> selectLocation(@RequestBody LocationRequestDto requestDto) {
        LocationResponseDto responseDto = locationService.selectGroupLocation(requestDto);
        return ResponseEntity.ok(responseDto);
    }
}
