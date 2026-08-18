package com.insighton.core.controller.internal;

import com.insighton.core.domain.actuatortypedefinition.dto.ActuatorTypeCreateRequest;
import com.insighton.core.domain.actuatortypedefinition.dto.ActuatorTypeResponse;
import com.insighton.core.domain.actuatortypedefinition.service.ActuatorTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/v1/actuator-type")
@RequiredArgsConstructor
public class ActuatorTypeController {

    private final ActuatorTypeService actuatorTypeService;

    @GetMapping
    public ResponseEntity<List<ActuatorTypeResponse>> getAllActuatorTypes() {
        return ResponseEntity.ok(actuatorTypeService.getAllActuatorTypes());
    }

    @PostMapping
    public ResponseEntity<Void> createActuatorType(@Valid @RequestBody ActuatorTypeCreateRequest request) {
        actuatorTypeService.createActuatorType(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{typeCode}")
    public ResponseEntity<Void> deleteActuatorType(@PathVariable String typeCode) {
        actuatorTypeService.deleteActuatorType(typeCode);
        return ResponseEntity.noContent().build();
    }
}