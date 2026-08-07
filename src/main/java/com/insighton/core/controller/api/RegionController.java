package com.insighton.core.controller.api;

import com.insighton.core.domain.region.dto.RegionRequestDto;
import com.insighton.core.domain.region.dto.RegionResponseDto;
import com.insighton.core.domain.region.service.RegionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/regions")
public class RegionController {

    private final RegionService locationService;

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

    @PostMapping("/select")
    public ResponseEntity<RegionResponseDto> selectLocation(@RequestBody RegionRequestDto requestDto) {
        RegionResponseDto responseDto = locationService.selectGroupLocation(requestDto);
        return ResponseEntity.ok(responseDto);
    }
}
