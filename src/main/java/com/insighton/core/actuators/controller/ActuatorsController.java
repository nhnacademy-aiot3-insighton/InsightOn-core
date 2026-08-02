package com.insighton.core.actuators.controller;

import com.insighton.core.actuators.dto.ActuatorNameUpdateRequest;
import com.insighton.core.actuators.dto.ActuatorsRequest;
import com.insighton.core.actuators.dto.ActuatorsResponse;
import com.insighton.core.actuators.service.ActuatorsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups/{groupsId}/actuators")
public class ActuatorsController {

    private final ActuatorsService actuatorsService;

    @PostMapping
    public ResponseEntity<Long> createActuator(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @Valid @RequestBody ActuatorsRequest request) {
        Long actuatorId = actuatorsService.createActuator(userId, groupsId, request);
        return ResponseEntity.ok(actuatorId);
    }

    @GetMapping("/{actuatorId}")
    public ResponseEntity<ActuatorsResponse> getActuatorById(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @PathVariable Long actuatorId) {
        return ResponseEntity.ok(actuatorsService.getActuatorById(userId, groupsId, actuatorId));
    }

    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<ActuatorsResponse>> getActuatorsByLocationId(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @PathVariable Long locationId) {
        return ResponseEntity.ok(actuatorsService.getActuatorsByLocationId(userId, groupsId, locationId));
    }

    @PutMapping("/{actuatorId}/state")
    public ResponseEntity<Void> updateActuatorState(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @PathVariable Long actuatorId,
            @RequestBody Map<String, Object> newState) {
        // 컨트롤러를 통한 사용자 요청이므로 isSystemRequest = false
        actuatorsService.updateActuatorState(userId, groupsId, actuatorId, newState, false);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{actuatorId}/name")
    public ResponseEntity<Void> updateActuatorName(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @PathVariable Long actuatorId,
            @Valid @RequestBody ActuatorNameUpdateRequest request) {
        actuatorsService.updateActuatorName(userId, groupsId, actuatorId, request.deviceName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{actuatorId}")
    public ResponseEntity<Void> deleteActuatorById(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @PathVariable Long actuatorId) {
        actuatorsService.deleteActuatorById(userId, groupsId, actuatorId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId) {
        actuatorsService.deleteAll(userId, groupsId);
        return ResponseEntity.ok().build();
    }
}