package com.insighton.core.controller.api;

import com.insighton.core.controller.swagger.ActuatorControllerApi;
import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogResponse;
import com.insighton.core.domain.actuators.dto.ActuatorNameUpdateRequest;
import com.insighton.core.domain.actuators.dto.ActuatorRequest;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import com.insighton.core.usecase.actuator.CreateActuatorUseCase;
import com.insighton.core.usecase.actuator.DeleteActuatorUseCase;
import com.insighton.core.usecase.actuator.DeleteAllActuatorUseCase;
import com.insighton.core.usecase.actuator.GetActuatorRunLogsUseCase;
import com.insighton.core.usecase.actuator.GetActuatorUseCase;
import com.insighton.core.usecase.actuator.GetActuatorsByLocationUseCase;
import com.insighton.core.usecase.actuator.UpdateActuatorNameUseCase;
import com.insighton.core.usecase.actuator.UpdateActuatorStateUseCase;
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
@RequestMapping("/api/v1/groups/{group-id}/actuators")
public class ActuatorController implements ActuatorControllerApi {

    private final CreateActuatorUseCase createActuatorUseCase;
    private final GetActuatorUseCase getActuatorUseCase;
    private final GetActuatorsByLocationUseCase getActuatorsByLocationUseCase;
    private final UpdateActuatorStateUseCase updateActuatorStateUseCase;
    private final UpdateActuatorNameUseCase updateActuatorNameUseCase;
    private final GetActuatorRunLogsUseCase getActuatorRunLogsUseCase;
    private final DeleteActuatorUseCase deleteActuatorUseCase;
    private final DeleteAllActuatorUseCase deleteAllActuatorUseCase;


    // 액추에이터 생성
    @Override
    @PostMapping
    public ResponseEntity<Long> createActuator(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupsId,
            @Valid @RequestBody ActuatorRequest request) {
        Long actuatorId = createActuatorUseCase.createActuator(userId, groupsId, request);
        return ResponseEntity.ok(actuatorId);
    }

    // 단일 액추에이터 조회
    @Override
    @GetMapping("/{actuator-id}")
    public ResponseEntity<ActuatorResponse> getActuatorById(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupsId,
            @PathVariable("actuator-id") Long actuatorId) {
        return ResponseEntity.ok(getActuatorUseCase.getActuatorById(userId, groupsId, actuatorId));
    }

    // 위치별 액추에이터 목록 조회
    @Override
    @GetMapping("/location/{location-id}")
    public ResponseEntity<List<ActuatorResponse>> getActuatorsByLocationId(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable ("group-id") Long groupsId,
            @PathVariable ("location-id") Long locationId) {
        return ResponseEntity.ok(getActuatorsByLocationUseCase.getActuatorsByLocationId(userId, groupsId, locationId));
    }

    // 유저 전용 액추에이터 업데이트
    @Override
    @PutMapping("/{actuator-id}/state")
    public ResponseEntity<Void> updateActuatorState(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id")Long groupsId,
            @PathVariable("actuator-id")Long actuatorId,
            @RequestBody Map<String, Object> newState) {
        updateActuatorStateUseCase.updateActuatorState(userId, groupsId, actuatorId, newState);
        return ResponseEntity.ok().build();
    }



    // 실행 이력 조회 - getActuatorById의 소유권/권한 검증을 그대로 재사용
    @Override
    @GetMapping("/{actuator-id}/logs")
    public ResponseEntity<Page<ActuatorRunLogResponse>> getActuatorRunLogs(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id")Long groupsId,
            @PathVariable("actuator-id")Long actuatorId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(getActuatorRunLogsUseCase.getActuatorRunLogs(userId, groupsId, actuatorId, pageable));
    }

    // 액추에이터 이름 수정
    @Override
    @PutMapping("/{actuator-id}/name")
    public ResponseEntity<Void> updateActuatorName(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id")Long groupsId,
            @PathVariable("actuator-id")Long actuatorId,
            @Valid @RequestBody ActuatorNameUpdateRequest request) {
        updateActuatorNameUseCase.updateActuatorName(userId, groupsId, actuatorId, request.sensorName());
        return ResponseEntity.noContent().build();
    }

    // 액추에이터 삭제
    @Override
    @DeleteMapping("/{actuator-id}")
    public ResponseEntity<Void> deleteActuatorById(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id")Long groupsId,
            @PathVariable("actuator-id")Long actuatorId) {
        deleteActuatorUseCase.deleteActuatorById(userId, groupsId, actuatorId);
        return ResponseEntity.noContent().build();
    }

    // 그룹 소속 액추에이터 전체 삭제
    @Override
    @DeleteMapping
    public ResponseEntity<Void> deleteAll(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id")Long groupsId) {
        deleteAllActuatorUseCase.deleteAll(userId, groupsId);
        return ResponseEntity.noContent().build();
    }



}
