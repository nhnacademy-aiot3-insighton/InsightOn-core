package com.insighton.core.controller.swagger;

import com.insighton.core.domain.gateway.dto.GatewayCreateRequest;
import com.insighton.core.domain.gateway.dto.GatewayResponse;
import com.insighton.core.domain.gateway.dto.GatewayUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

/**
 * Swagger 문서화 전용 인터페이스 - 게이트웨이(Gateway) 관리
 */
@Tag(name = "게이트웨이 API (Gateway)", description = "게이트웨이 생성, 조회, 수정, 삭제 API - 그룹당 게이트웨이 1개(1:1)")
public interface GatewayControllerApi {

    @Operation(summary = "게이트웨이 생성", description = "그룹에 새로운 게이트웨이를 등록합니다. (Manager 이상 권한 필요, 그룹당 1개만 등록 가능)")
    @ApiResponse(responseCode = "200", description = "게이트웨이 생성 성공")
    @ApiResponse(responseCode = "400", description = "connection_config에 brokerUrls가 없거나 형식이 잘못됨")
    @ApiResponse(responseCode = "403", description = "게이트웨이 생성 권한 없음")
    @ApiResponse(responseCode = "409", description = "이미 게이트웨이가 등록된 그룹")
    ResponseEntity<GatewayResponse> create(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Valid GatewayCreateRequest request
    );

    @Operation(summary = "게이트웨이 단건 조회", description = "게이트웨이 ID로 상세 정보를 조회합니다. Manager 이상은 connection_config(브로커 인증정보 포함)까지 함께 받습니다.")
    @ApiResponse(responseCode = "200", description = "게이트웨이 조회 성공")
    @ApiResponse(responseCode = "403", description = "다른 그룹의 게이트웨이에 접근")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 게이트웨이")
    ResponseEntity<GatewayResponse> getById(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "조회할 게이트웨이 ID", required = true) Long gatewayId
    );

    @Operation(summary = "그룹의 게이트웨이 조회", description = "그룹당 게이트웨이가 1개이므로, groupId로 그 게이트웨이를 바로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "게이트웨이 조회 성공")
    @ApiResponse(responseCode = "403", description = "다른 그룹의 게이트웨이에 접근")
    @ApiResponse(responseCode = "404", description = "해당 그룹에 등록된 게이트웨이 없음")
    ResponseEntity<GatewayResponse> getByGroupId(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "조회 대상 그룹 ID", required = true) Long groupId
    );

    @Operation(summary = "전체 게이트웨이 조회 (관리자)", description = "시스템에 등록된 전체 게이트웨이를 페이지 단위로 조회합니다. (ADMIN 전용)")
    @ApiResponse(responseCode = "200", description = "게이트웨이 목록 조회 성공")
    @ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    ResponseEntity<Page<GatewayResponse>> getAll(
            @Parameter(description = "요청자 role (ADMIN만 허용)", required = true) String userRole,
            Pageable pageable
    );

    @Operation(summary = "게이트웨이 수정", description = "이름/프로토콜/connection_config를 부분 수정합니다. 브로커 주소가 바뀌면 소속 센서가 전부 정리됩니다.")
    @ApiResponse(responseCode = "204", description = "게이트웨이 수정 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 값(빈 이름, 프로토콜 변경 시 connectionConfig 누락 등)")
    @ApiResponse(responseCode = "403", description = "게이트웨이 수정 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 게이트웨이")
    ResponseEntity<Void> update(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "수정할 게이트웨이 ID", required = true) Long gatewayId,
            GatewayUpdateRequest request
    );

    @Operation(summary = "게이트웨이 삭제", description = "게이트웨이를 삭제합니다. 소속된 센서/센서 어트리뷰트도 함께 정리됩니다.")
    @ApiResponse(responseCode = "204", description = "게이트웨이 삭제 성공")
    @ApiResponse(responseCode = "403", description = "게이트웨이 삭제 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 게이트웨이")
    ResponseEntity<Void> delete(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "삭제할 게이트웨이 ID", required = true) Long gatewayId
    );
}
