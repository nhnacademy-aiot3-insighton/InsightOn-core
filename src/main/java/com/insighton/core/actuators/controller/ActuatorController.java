package com.insighton.core.actuators.controller;

import com.insighton.core.actuator_run_logs.dto.ActuatorRunLogResponse;
import com.insighton.core.actuator_run_logs.entity.ExecutedByType;
import com.insighton.core.actuator_run_logs.service.ActuatorRunLogService;
import com.insighton.core.actuators.dto.ActuatorNameUpdateRequest;
import com.insighton.core.actuators.dto.ActuatorRequest;
import com.insighton.core.actuators.dto.ActuatorResponse;
import com.insighton.core.actuators.exception.CouldNotAbleToUpdateByUserToSystem;
import com.insighton.core.actuators.service.ActuatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups/{groupsId}/actuators")
public class ActuatorController {

    private final ActuatorService actuatorService; // 액추에이터 서비스
    private final ActuatorRunLogService actuatorRunLogService; // 실행 로그 조회용


    // 액추에이터 생성
    @PostMapping
    public ResponseEntity<Long> createActuator(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @Valid @RequestBody ActuatorRequest request) {
        Long actuatorId = actuatorService.createActuator(userId, groupsId, request);
        return ResponseEntity.ok(actuatorId);
    }

    // 단일 액추에이터 조회
    @GetMapping("/{actuatorId}")
    public ResponseEntity<ActuatorResponse> getActuatorById(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @PathVariable Long actuatorId) {
        return ResponseEntity.ok(actuatorService.getActuatorById(userId, groupsId, actuatorId));
    }

    // 위치별 액추에이터 목록 조회
    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<ActuatorResponse>> getActuatorsByLocationId(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @PathVariable Long locationId) {
        return ResponseEntity.ok(actuatorService.getActuatorsByLocationId(userId, groupsId, locationId));
    }

    // 유저 전용 액추에이터 업데이트
    @PutMapping("/{actuatorId}/state")
    public ResponseEntity<Void> updateActuatorState(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @PathVariable Long actuatorId,
            @RequestBody Map<String, Object> newState) {
        // 컨트롤러를 통한 사용자 요청이므로 isSystemRequest = false
        actuatorService.updateActuatorState(userId, groupsId, actuatorId, newState, ExecutedByType.USER);
        return ResponseEntity.ok().build();
    }

    // 룰엔진/AI 등 내부 시스템 전용 액추에이터 업데이트
    @PutMapping("/internal/{actuatorId}/state")
    public ResponseEntity<Void> updateActuatorStateBySystem(
            @PathVariable Long actuatorId,
            @RequestParam ExecutedByType executedByType,
            @RequestBody Map<String, Object> newState){
        if(executedByType == ExecutedByType.USER){
            throw new CouldNotAbleToUpdateByUserToSystem("내부 API는 USER타입으로 호출할 수 없습니다");
        }
        actuatorService.updateActuatorState(null, null, actuatorId, newState,executedByType);
        return ResponseEntity.ok().build();
    }

    // 실행 이력 조회 - getActuatorById의 소유권/권한 검증을 그대로 재사용
    @GetMapping("/{actuatorId}/logs")
    public ResponseEntity<Page<ActuatorRunLogResponse>> getActuatorRunLogs(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @PathVariable Long actuatorId,
            @PageableDefault(size = 20)Pageable pageable){
        actuatorService.getActuatorById(userId, groupsId, actuatorId);
        return ResponseEntity.ok(actuatorRunLogService.getRunLogsByActuatorId(actuatorId, pageable));

    }

    // 액추에이터 이름 수정
    @PutMapping("/{actuatorId}/name")
    public ResponseEntity<Void> updateActuatorName(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @PathVariable Long actuatorId,
            @Valid @RequestBody ActuatorNameUpdateRequest request) {
        actuatorService.updateActuatorName(userId, groupsId, actuatorId, request.deviceName());
        return ResponseEntity.ok().build();
    }

    // 액추에이터 삭제
    @DeleteMapping("/{actuatorId}")
    public ResponseEntity<Void> deleteActuatorById(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId,
            @PathVariable Long actuatorId) {
        actuatorService.deleteActuatorById(userId, groupsId, actuatorId);
        return ResponseEntity.ok().build();
    }

    // 그룹 소속 액추에이터 전체 삭제
    @DeleteMapping
    public ResponseEntity<Void> deleteAll(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long groupsId) {
        actuatorService.deleteAll(userId, groupsId);
        return ResponseEntity.ok().build();
    }
}