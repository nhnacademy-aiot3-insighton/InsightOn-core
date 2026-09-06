package com.insighton.core.controller.swagger;

import com.insighton.core.domain.groupregistration.dto.CreateGroupRegistrationRequest;
import com.insighton.core.domain.groupregistration.dto.GroupRegistrationResponse;
import com.insighton.core.domain.groupregistration.entity.GroupRegistrationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "그룹 등록 신청 API (GroupRegistration)", description = "그룹 생성 신청, 목록/상세 조회, 취소 및 승인/거절 API")
public interface GroupRegistrationControllerApi {

    @Operation(summary = "그룹 생성 신청", description = "새로운 그룹 생성을 신청합니다.")
    @ApiResponse(responseCode = "201", description = "그룹 생성 신청 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 입력 정보")
    @ApiResponse(responseCode = "409", description = "이미 처리 대기 중인 신청이 존재하는 경우")
    ResponseEntity<GroupRegistrationResponse> createRequest(
            @Parameter(description = "신청자 유저 ID", required = true) Long requesterId,
            @Valid CreateGroupRegistrationRequest registrationRequest
    );

    @Operation(summary = "전체 그룹 등록 신청 목록 조회 (관리자용)", description = "시스템 관리자가 전체 그룹 등록 신청 목록을 상태별로 페이징 조회합니다.")
    @ApiResponse(responseCode = "200", description = "그룹 등록 신청 목록 조회 성공")
    @ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    ResponseEntity<Page<GroupRegistrationResponse>> getGroupRegistrations(
            @Parameter(description = "유저 권한 (ROLE_SYSTEM_ADMIN)") String userRole,
            @Parameter(description = "조회할 신청 상태 (미지정 시 전체 조회)") GroupRegistrationStatus status,
            Pageable pageable
    );

    @Operation(summary = "내 그룹 등록 신청 목록 조회", description = "로그인한 사용자가 신청한 그룹 등록 신청 목록을 페이징 조회합니다.")
    @ApiResponse(responseCode = "200", description = "내 그룹 등록 신청 목록 조회 성공")
    ResponseEntity<Page<GroupRegistrationResponse>> getMyGroupRegistrations(
            @Parameter(description = "신청자 유저 ID", required = true) Long requesterId,
            Pageable pageable
    );

    @Operation(summary = "그룹 등록 신청 상세 조회", description = "특정 그룹 등록 신청의 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "그룹 등록 신청 상세 조회 성공")
    @ApiResponse(responseCode = "403", description = "해당 신청 접근 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 신청")
    ResponseEntity<GroupRegistrationResponse> getGroupRegistration(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "유저 권한 (ROLE_SYSTEM_ADMIN)") String userRole,
            @Parameter(description = "조회할 그룹 등록 신청 ID", required = true) Long groupRegistrationId
    );

    @Operation(summary = "그룹 등록 신청 취소", description = "신청자가 자신이 신청한 그룹 등록 신청을 취소합니다.")
    @ApiResponse(responseCode = "200", description = "그룹 등록 신청 취소 성공")
    @ApiResponse(responseCode = "403", description = "취소 권한 없음 (신청자 본인만 가능)")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 신청")
    @ApiResponse(responseCode = "409", description = "이미 처리된 신청은 취소 불가")
    ResponseEntity<Void> cancelGroupRegistration(
            @Parameter(description = "신청자 유저 ID", required = true) Long requesterId,
            @Parameter(description = "취소할 그룹 등록 신청 ID", required = true) Long groupRegistrationId
    );

    @Operation(summary = "그룹 등록 신청 승인 (관리자용)", description = "관리자가 그룹 등록 신청을 승인합니다. 승인과 동시에 Group 생성까지 이어집니다.")
    @ApiResponse(responseCode = "200", description = "그룹 등록 신청 승인 성공")
    @ApiResponse(responseCode = "403", description = "승인 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 신청")
    @ApiResponse(responseCode = "409", description = "이미 처리된 신청")
    ResponseEntity<Void> approveGroupRegistration(
            @Parameter(description = "유저 권한 (ROLE_SYSTEM_ADMIN)") String userRole,
            @Parameter(description = "승인자 유저 ID", required = true) Long approverId,
            @Parameter(description = "승인할 그룹 등록 신청 ID", required = true) Long groupRegistrationId
    );

    @Operation(summary = "그룹 등록 신청 거절 (관리자용)", description = "관리자가 그룹 등록 신청을 거절합니다.")
    @ApiResponse(responseCode = "200", description = "그룹 등록 신청 거절 성공")
    @ApiResponse(responseCode = "403", description = "거절 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 신청")
    @ApiResponse(responseCode = "409", description = "이미 처리된 신청")
    ResponseEntity<Void> rejectGroupRegistration(
            @Parameter(description = "유저 권한 (ROLE_SYSTEM_ADMIN)") String userRole,
            @Parameter(description = "거절자 유저 ID", required = true) Long approverId,
            @Parameter(description = "거절할 그룹 등록 신청 ID", required = true) Long groupRegistrationId
    );
}
