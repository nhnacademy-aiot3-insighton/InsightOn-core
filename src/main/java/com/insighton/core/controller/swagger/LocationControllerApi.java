package com.insighton.core.controller.swagger;

import com.insighton.core.domain.location.dto.request.LocationCreateRequest;
import com.insighton.core.domain.location.dto.request.LocationUpdateRequest;
import com.insighton.core.domain.location.dto.response.LocationListResponse;
import com.insighton.core.domain.location.dto.response.LocationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Swagger 문서화 전용 인터페이스 - 위치(Location) 관리
 */
@Tag(name = "위치 API (Location)", description = "위치 생성, 목록/단건 조회, 제어 모드 설정, 명칭 변경 및 삭제 API")
public interface LocationControllerApi {

    @Operation(summary = "위치 생성", description = "그룹 내 새로운 위치(Location)를 생성합니다. (Manager 이상 권한 필요)")
    @ApiResponse(responseCode = "201", description = "위치 생성 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 입력 요청")
    @ApiResponse(responseCode = "403", description = "위치 생성 권한 없음")
    @ApiResponse(responseCode = "409", description = "이미 동일한 이름의 위치가 존재하는 경우")
    ResponseEntity<Void> createLocation(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Valid LocationCreateRequest request
    );

    @Operation(summary = "위치 목록 조회", description = "특정 그룹에 등록된 전체 위치 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "위치 목록 조회 성공")
    @ApiResponse(responseCode = "403", description = "해당 그룹 위치 목록 접근 권한 없음")
    ResponseEntity<List<LocationListResponse>> getLocationList(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId
    );

    @Operation(summary = "위치 상세 조회", description = "특정 위치의 상세 정보 및 모드 설정을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "위치 상세 정보 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 위치")
    ResponseEntity<LocationResponse> getLocation(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "조회 대상 위치 ID", required = true) Long locationId
    );

    @Operation(summary = "자동 제어 모드 토글", description = "위치 내 액추에이터 자동 제어 모드(SUGGESTION, AUTO 등)를 변경합니다.")
    @ApiResponse(responseCode = "200", description = "제어 모드 변경 성공")
    @ApiResponse(responseCode = "403", description = "제어 모드 변경 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 위치")
    ResponseEntity<Void> toggleAutoControlMode(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "위치 ID", required = true) Long locationId
    );

    @Operation(summary = "위치 이름 변경", description = "위치의 명칭을 변경합니다.")
    @ApiResponse(responseCode = "200", description = "위치 이름 변경 성공")
    @ApiResponse(responseCode = "400", description = "올바르지 않은 위치 이름")
    @ApiResponse(responseCode = "403", description = "이름 변경 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 위치")
    @ApiResponse(responseCode = "409", description = "이미 동일한 위치 이름이 사용 중인 경우")
    ResponseEntity<Void> updateName(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "위치 ID", required = true) Long locationId,
            @Valid LocationUpdateRequest request
    );

    @Operation(summary = "위치 삭제", description = "특정 위치를 삭제합니다. (연관된 대시보드 및 위젯 함께 삭제)")
    @ApiResponse(responseCode = "204", description = "위치 삭제 성공")
    @ApiResponse(responseCode = "403", description = "위치 삭제 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 위치")
    ResponseEntity<Void> deleteLocation(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "삭제할 위치 ID", required = true) Long locationId
    );
}
