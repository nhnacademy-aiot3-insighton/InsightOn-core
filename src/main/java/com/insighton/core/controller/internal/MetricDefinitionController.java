package com.insighton.core.controller.internal;

import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionCreateRequest;
import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionResponse;
import com.insighton.core.usecase.sensorattribute.CreateMetricUseCase;
import com.insighton.core.usecase.sensorattribute.GetAllMetricUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/v1/metric-definitions")
@RequiredArgsConstructor
public class MetricDefinitionController {

    private final GetAllMetricUseCase getAllMetricDefinition;
    private final CreateMetricUseCase createMetricDefinition;

    @GetMapping
    public ResponseEntity<List<MetricDefinitionResponse>> getAllMetricDefinitions(){
        return ResponseEntity.ok(getAllMetricDefinition.getAllMetric());
    }

    @PostMapping
    public ResponseEntity<Void> createMetricDefinition(@Valid @RequestBody MetricDefinitionCreateRequest request) {
        createMetricDefinition.createMetric(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}