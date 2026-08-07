package com.insighton.core.sensor_attributes.controller;

import com.insighton.core.sensor_attributes.dto.MetricDefinitionCreateRequest;
import com.insighton.core.sensor_attributes.dto.MetricDefinitionResponse;
import com.insighton.core.sensor_attributes.service.SensorAttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metric-definitions")
@RequiredArgsConstructor
public class MetricDefinitionController {

    private final SensorAttributeService attributeService;

    @GetMapping
    public ResponseEntity<List<MetricDefinitionResponse>> getAllMetricDefinitions(){
        return ResponseEntity.ok(attributeService.getAllMetricDefinitions());
    }

    @PostMapping
    public ResponseEntity<Void> createMetricDefinition(@Valid @RequestBody MetricDefinitionCreateRequest request) {
        attributeService.createMetricDefinition(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}