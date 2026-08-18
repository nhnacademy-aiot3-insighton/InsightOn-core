package com.insighton.core.controller.api;

import com.insighton.core.domain.location.dto.request.LocationCreateRequest;
import com.insighton.core.domain.location.dto.request.LocationUpdateRequest;
import com.insighton.core.domain.location.dto.response.LocationListResponse;
import com.insighton.core.domain.location.dto.response.LocationResponse;
import com.insighton.core.usecase.location.LocationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups/{group-id}/location")
public class LocationController {
    private final LocationUseCase useCase;

    /**
     * location 생성
     *
     * @param userId  location을 생성하려는 user의 ID
     * @param groupId Location이 속할 그룹
     * @param request location 생성 요청
     * @return 성공 시 상태 201 반환
     */
    @PostMapping("/create")
    public ResponseEntity<Void> createLocation(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @Valid @RequestBody LocationCreateRequest request
    ) {
        useCase.createLocation(userId, groupId, request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * location list 조회
     *
     * @param userId  조회하려는 user의 ID
     * @param groupId location list들이 속해있는 group ID
     * @return location list 반환
     */
    @GetMapping("/list")
    public ResponseEntity<List<LocationListResponse>> getLocationList(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId
    ) {
        List<LocationListResponse> locationListResponses = useCase.getLocationList(userId, groupId);

        return ResponseEntity.ok(locationListResponses);
    }

    /**
     * Location 상세 조회
     *
     * @param userId     상세 조회 하려는 user의 ID
     * @param groupId    상세 조회 하려는 Location이 속한 group의 ID
     * @param locationId 상세 조회 할 location ID
     * @return location 상세 정보 반환
     */
    @GetMapping("/{location-id}")
    public ResponseEntity<LocationResponse> getLocation(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("location-id") Long locationId
    ) {
        LocationResponse response = useCase.getLocation(userId, groupId, locationId);

        return ResponseEntity.ok(response);
    }

    /**
     * AutoControlMode 변경
     *
     * @param userId     변경하려는 user의 ID
     * @param groupId    변경하려는 location이 속한 group의 ID
     * @param locationId 변경하려는 location의 ID
     * @return 성공 시 상태 200 반환
     */
    @PutMapping("/{location-id}/toggle-mode")
    public ResponseEntity<Void> toggleAutoControlMode(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("location-id") Long locationId
    ) {
        useCase.toggleAutoControlMode(userId, groupId, locationId);

        return ResponseEntity.ok().build();
    }

    /**
     * location의 name 변경
     *
     * @param userId     변경하려는 user의 ID
     * @param groupId    변경하려는 location이 속한 group의 ID
     * @param locationId 변경하려는 location의 ID
     * @param request    변경하려는 name이 담겨있는 DTO(?)
     * @return 성공 시 상태 200 반환
     */
    @PutMapping("/{location-id}/update")
    public ResponseEntity<Void> updateName(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("location-id") Long locationId,
            @Valid @RequestBody LocationUpdateRequest request) {

        useCase.updateName(userId, groupId, locationId, request);

        return ResponseEntity.ok().build();
    }

    /**
     * location 삭제
     *
     * @param userId     삭제하려는 user의 ID
     * @param groupId    삭제하려는 location이 속한 group의 ID
     * @param locationId 삭제하려는 location의 ID
     * @return 성공 시 상태 204 반환
     */
    @DeleteMapping("/{location-id}/delete")
    public ResponseEntity<Void> deleteLocation(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("location-id") Long locationId) {
        useCase.deleteLocation(userId, groupId, locationId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
