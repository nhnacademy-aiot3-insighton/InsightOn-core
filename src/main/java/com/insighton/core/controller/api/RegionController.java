package com.insighton.core.controller.api;

import com.insighton.core.domain.region.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/regions")
public class RegionController {

    private final RegionService regionService;

    // 광역시/도
    @GetMapping("/states")
    public ResponseEntity<List<String>> getAllStates() {
        List<String> states = regionService.getSortedStates();
        return ResponseEntity.ok(states);
    }

    // 시/군/구
    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCitiesByState(@RequestParam String state) {
        List<String> cities = regionService.getSortedCities(state);
        return ResponseEntity.ok(cities);
    }
}
