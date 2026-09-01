package com.insighton.core.controller.swagger;

import com.insighton.core.domain.groupmember.dto.response.MyGroupIdResponse;
import com.insighton.core.domain.groups.dto.request.GroupRequest;
import com.insighton.core.domain.groups.dto.response.GroupAdminResponse;
import com.insighton.core.domain.groups.dto.response.GroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

/**
 * Swagger 문서화 전용 인터페이스 - 그룹(Group) 관리
 */
@Tag(name = "그룹 API (Group)", description = "그룹 생성, 정보 조회, 초대 토큰 발급, 가입 및 수정/삭제 API")
public interface GroupControllerApi {

    @Operation(summary = "로그인 유저 소속 그룹 ID 조회", description = "로그인한 유저가 소속된 그룹의 ID를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "소속 그룹 ID 조회 성공")
    @ApiResponse(responseCode = "404", description = "소속된 그룹을 찾을 수 없음")
    ResponseEntity<MyGroupIdResponse> getMyGroupId(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId
    );

    @Operation(summary = "그룹 생성", description = "새로운 그룹을 생성합니다.")
    @ApiResponse(responseCode = "201", description = "그룹 생성 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 입력 정보")
    @ApiResponse(responseCode = "409", description = "이미 그룹에 소속되어 있거나 중복 신청된 경우")
    ResponseEntity<Void> createGroup(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Valid GroupRequest groupsCreateRequest
    );

    @Operation(summary = "내 그룹 정보 조회", description = "현재 사용자가 소속된 그룹의 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "그룹 정보 조회 성공")
    @ApiResponse(responseCode = "403", description = "해당 그룹 접근 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹")
    ResponseEntity<GroupResponse> getMyGroup(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "조회할 그룹 ID", required = true) Long groupId
    );

    @Operation(summary = "초대받은 그룹 미리보기", description = "초대 토큰을 통해 초대받은 그룹의 요약 정보를 사전 조회합니다.")
    @ApiResponse(responseCode = "200", description = "그룹 미리보기 조회 성공")
    @ApiResponse(responseCode = "404", description = "유효하지 않거나 만료된 초대 토큰")
    ResponseEntity<GroupResponse> getGroupPreview(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "초대 토큰", required = true) String inviteToken
    );

    @Operation(summary = "초대 토큰으로 그룹 가입", description = "초대 토큰을 사용하여 특정 그룹에 가입을 요청합니다.")
    @ApiResponse(responseCode = "200", description = "그룹 가입 성공")
    @ApiResponse(responseCode = "400", description = "잘못되었거나 만료된 초대 코드")
    @ApiResponse(responseCode = "409", description = "이미 해당 그룹에 가입되어 있는 경우")
    ResponseEntity<Void> joinGroup(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "초대 토큰", required = true) String inviteToken
    );

    @Operation(summary = "[시스템 관리자] 전체 그룹 목록 조회", description = "시스템 관리자(ROLE_SYSTEM_ADMIN)용 전체 그룹 목록을 페이징 조회합니다.")
    @ApiResponse(responseCode = "200", description = "그룹 목록 조회 성공")
    @ApiResponse(responseCode = "403", description = "시스템 관리자 권한 없음")
    ResponseEntity<Page<GroupAdminResponse>> getGroupList(
            @Parameter(description = "유저 권한 (ROLE_SYSTEM_ADMIN)", required = true) String userRole,
            @Parameter(description = "시스템 관리자 유저 ID", required = true) Long userId,
            Pageable pageable
    );

    @Operation(summary = "그룹 초대 토큰 재발급", description = "그룹의 초대 토큰을 새로 재발급합니다. (Manager 이상 권한 필요)")
    @ApiResponse(responseCode = "200", description = "초대 토큰 재발급 성공")
    @ApiResponse(responseCode = "403", description = "재발급 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹")
    ResponseEntity<Void> newInviteToken(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId
    );

    @Operation(summary = "그룹 정보 수정", description = "그룹 명칭 및 설명 등 그룹 상세 정보를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "그룹 정보 수정 성공")
    @ApiResponse(responseCode = "403", description = "수정 권한 없음")
    @ApiResponse(responseCode = "409", description = "이미 존재하는 그룹 이름")
    ResponseEntity<Void> updateGroup(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "수정할 그룹 ID", required = true) Long groupId,
            @Valid GroupRequest request
    );

    @Operation(summary = "그룹 삭제", description = "그룹을 완전히 삭제 처리합니다. (SuperManager만 가능)")
    @ApiResponse(responseCode = "204", description = "그룹 삭제 성공")
    @ApiResponse(responseCode = "403", description = "삭제 권한 없음 (SuperManager만 가능)")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹")
    ResponseEntity<Void> deleteGroup(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "삭제할 그룹 ID", required = true) Long groupId,
            @Parameter(description = "확인용 초대 토큰", required = true) String inviteToken
    );
}
