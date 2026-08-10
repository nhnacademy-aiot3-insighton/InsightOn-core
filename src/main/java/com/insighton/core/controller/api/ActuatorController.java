package com.insighton.core.controller.api;

import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogResponse;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuatorrunlogs.service.ActuatorRunLogService;
import com.insighton.core.domain.actuators.dto.ActuatorNameUpdateRequest;
import com.insighton.core.domain.actuators.dto.ActuatorRequest;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import com.insighton.core.domain.actuators.service.ActuatorService;
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
@RequestMapping("/api/v1/groups/{group-id}/actuators")// 액추에이터는 항상 특정 그룹 소속이라 group-id를 경로에 강제
public class ActuatorController {

    private final ActuatorService actuatorService; // 액추에이터 서비스
    private final ActuatorRunLogService actuatorRunLogService; // 실행 로그 조회용


    // 액추에이터 생성
    @PostMapping
    public ResponseEntity<Long> createActuator(
            @RequestHeader("X-USER-ID") Long userId, // 로그인 사용자 식별
            @PathVariable("group-id") Long groupsId,
            @Valid @RequestBody ActuatorRequest request) { // @Valid로 필수값 누락을 컨트롤러 진입 전에 400으로 차단
        Long actuatorId = actuatorService.createActuator(userId, groupsId, request);
        return ResponseEntity.ok(actuatorId); // 생성된 PK만 반환 - 클라이언트가 뒤이어 상세조회 호출하는 흐름 전제
    }

    // 단일 액추에이터 조회
    @GetMapping("/{actuator-id}")
    public ResponseEntity<ActuatorResponse> getActuatorById(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupsId,
            @PathVariable("actuator-id") Long actuatorId) {
        return ResponseEntity.ok(actuatorService.getActuatorById(userId, groupsId, actuatorId));
    }

    // 위치별 액추에이터 목록 조회
    @GetMapping("/location/{location-id}")
    public ResponseEntity<List<ActuatorResponse>> getActuatorsByLocationId(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable ("group-id") Long groupsId,
            @PathVariable ("location-id") Long locationId) {
        // 대시보드가 "이 방의 액추에이터 전부"를 한 화면에 보여줘야 해서 위치 단위 목록 조회가 필요
        return ResponseEntity.ok(actuatorService.getActuatorsByLocationId(userId, groupsId, locationId));
    }

    // 유저 전용 액추에이터 업데이트
    @PutMapping("/{actuator-id}/state")
    public ResponseEntity<Void> updateActuatorState(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id")Long groupsId,
            @PathVariable("actuator-id")Long actuatorId,
            @RequestBody Map<String, Object> newState) {
        // 명령 종류가 여러가지라 고정 DTO 대신 Map으로 유연하게 받음
        // 사람이 직접 호출하는 유일한 경로라 ExecutedByType.USER를 하드코딩 - 클라이언트가 이 값을 조작할 수 없게 막는 목적
        // 컨트롤러를 통한 사용자 요청이므로 isSystemRequest = false
        actuatorService.updateActuatorState(userId, groupsId, actuatorId, newState, ExecutedByType.USER);
        return ResponseEntity.ok().build();
    }



    // 실행 이력 조회 - getActuatorById의 소유권/권한 검증을 그대로 재사용
    @GetMapping("/{actuator-id}/logs")
    public ResponseEntity<Page<ActuatorRunLogResponse>> getActuatorRunLogs(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id")Long groupsId,
            @PathVariable("actuator-id")Long actuatorId,
            @PageableDefault(size = 20) Pageable pageable) {
        actuatorService.getActuatorById(userId, groupsId, actuatorId);
        return ResponseEntity.ok(actuatorRunLogService.getRunLogsByActuatorId(actuatorId, pageable));

    }

    // 액추에이터 이름 수정
    @PutMapping("/{actuator-id}/name")
    public ResponseEntity<Void> updateActuatorName(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id")Long groupsId,
            @PathVariable("actuator-id")Long actuatorId,
            @Valid @RequestBody ActuatorNameUpdateRequest request) {
        actuatorService.updateActuatorName(userId, groupsId, actuatorId, request.sensorName());
        return ResponseEntity.ok().build();
    }

    // 액추에이터 삭제
    @DeleteMapping("/{actuator-id}")
    public ResponseEntity<Void> deleteActuatorById(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id")Long groupsId,
            @PathVariable("actuator-id")Long actuatorId) {
        actuatorService.deleteActuatorById(userId, groupsId, actuatorId);
        return ResponseEntity.ok().build();
    }

    // 그룹 소속 액추에이터 전체 삭제
    @DeleteMapping
    public ResponseEntity<Void> deleteAll(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id")Long groupsId) {
        // 그룹 탈퇴/해체 시 그룹 소속 액추에이터를 한 번에 정리하기 위한 엔드포인트
        actuatorService.deleteAll(userId, groupsId);
        return ResponseEntity.ok().build();
    }



}