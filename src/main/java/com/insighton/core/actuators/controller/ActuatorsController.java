package com.insighton.core.actuators.controller;


import com.insighton.core.actuators.dto.ActuatorNameUpdateRequest;
import com.insighton.core.actuators.dto.ActuatorStateUpdateRequest;
import com.insighton.core.actuators.dto.ActuatorsRequest;
import com.insighton.core.actuators.dto.ActuatorsResponse;
import com.insighton.core.actuators.service.ActuatorsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/actuators")
@RequiredArgsConstructor
public class ActuatorsController {

    private final ActuatorsService actuatorsService;

    // 액추에이터 신규 생성 API (POST /api/v1/actuators)
    @PostMapping
    public ResponseEntity<Long> createActuator(@RequestBody @Valid ActuatorsRequest request){
        Long actuatorId = actuatorsService.createActuator(request);
        return ResponseEntity.ok(actuatorId);
    }

    // 액추에이터 상세 조회 API (GET /api/v1/actuators/{id})
    @GetMapping("/{id}")
    public ResponseEntity<ActuatorsResponse> getActuator(@PathVariable("id") Long id){
        ActuatorsResponse response = actuatorsService.getActuatorById(id);
        return ResponseEntity.ok(response);
    }

    // 구역별(locationId) 목록 조회 API
    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<ActuatorsResponse>> getActuatorsByLocation(@PathVariable("locationId") Long locationId){
        List<ActuatorsResponse> responses = actuatorsService.getActuatorsByLocationId(locationId);
        return ResponseEntity.ok(responses);
    }

    // 액추에이터 이름 수정 API
    @PutMapping("/{id}/name")
    public ResponseEntity<Void> updateActuatorName(
            @PathVariable("id") Long id,
            @RequestBody @Valid ActuatorNameUpdateRequest request){
        actuatorsService.updateActuatorName(id,request.deviceName());
        return ResponseEntity.noContent().build();
    }

    // 액추에이터 제어 명령 수정 API
    @PutMapping("/{id}/state")
    public ResponseEntity<Void> updateActuatorState(
            @PathVariable("id") Long id,
            @RequestBody @Valid ActuatorStateUpdateRequest request){
        actuatorsService.updateActuatorState(id,request.currentState());
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> deleteActuator(@PathVariable("id") Long id){
        actuatorsService.deleteActuatorById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/deleteAll")
    public ResponseEntity<Void> deleteAllActuator(){
        actuatorsService.deleteAll();
        return ResponseEntity.noContent().build();
    }


}
