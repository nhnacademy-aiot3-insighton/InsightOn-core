package com.insighton.core.controller.swagger;

import com.insighton.core.domain.groupmember.dto.response.GroupMemberListResponse;
import com.insighton.core.domain.groupmember.dto.response.GroupMemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Swagger 문서화 전용 인터페이스 - 그룹 멤버(GroupMember) 관리
 */
@Tag(name = "그룹 멤버 API (GroupMember)", description = "그룹 멤버 초대, 목록 조회, 상세 조회, 권한 변경 및 추방/탈퇴 API")
public interface GroupMemberControllerApi {

    @Operation(summary = "이메일로 그룹 멤버 초대", description = "이메일 주소를 기반으로 사용자를 그룹 멤버로 바로 초대/추가합니다.")
    @ApiResponse(responseCode = "200", description = "초대 성공")
    @ApiResponse(responseCode = "400", description = "유효하지 않은 이메일이거나 초대 조건 미충족")
    @ApiResponse(responseCode = "403", description = "멤버 초대 권한 없음 (Manager 이상 필요)")
    @ApiResponse(responseCode = "404", description = "해당 이메일의 유저를 찾을 수 없음")
    ResponseEntity<Void> inviteMemberByEmail(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "초대 대상 이메일", required = true) String email
    );

    @Operation(summary = "그룹 멤버 목록 조회", description = "현재 그룹에 속한 전체 멤버 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "그룹 멤버 목록 조회 성공")
    @ApiResponse(responseCode = "403", description = "그룹 멤버 목록 접근 권한 없음")
    ResponseEntity<List<GroupMemberListResponse>> getGroupMemberList(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId
    );

    @Operation(summary = "그룹 멤버 상세 조회", description = "특정 그룹 멤버의 상세 정보 및 권한을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "멤버 상세 조회 성공")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 그룹 멤버")
    ResponseEntity<GroupMemberResponse> getGroupMember(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "조회 대상 멤버 ID", required = true) Long groupMemberId
    );

    @Operation(summary = "매니저(Manager) 권한 토글", description = "일반 멤버와 매니저 권한 간 전환을 수행합니다. (SuperManager만 수행 가능)")
    @ApiResponse(responseCode = "200", description = "매니저 권한 변경 성공")
    @ApiResponse(responseCode = "400", description = "변경 불가 대상")
    @ApiResponse(responseCode = "403", description = "권한 변경 실패 (SuperManager 필요)")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 멤버")
    ResponseEntity<Void> toggleManagerRole(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "대상 멤버 ID", required = true) Long groupMemberId
    );

    @Operation(summary = "슈퍼매니저(SuperManager) 권한 양도", description = "현재 SuperManager가 자신의 대표 관리자 권한을 타 멤버에게 양도합니다.")
    @ApiResponse(responseCode = "200", description = "SuperManager 권한 양도 성공")
    @ApiResponse(responseCode = "400", description = "Manager 이상 역할을 가진 멤버에게만 양도 가능")
    @ApiResponse(responseCode = "403", description = "양도 권한 없음 (현재 SuperManager만 가능)")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 멤버")
    ResponseEntity<Void> toggleSuperManagerRole(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "양도 대상 멤버 ID", required = true) Long groupMemberId
    );

    @Operation(summary = "그룹 멤버 추방", description = "관리자가 특정 멤버를 그룹에서 강제 추방합니다.")
    @ApiResponse(responseCode = "204", description = "멤버 추방 성공")
    @ApiResponse(responseCode = "400", description = "자기 자신 또는 SuperManager는 추방 불가")
    @ApiResponse(responseCode = "403", description = "멤버 추방 권한 없음")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 멤버")
    ResponseEntity<Void> kickGroupMember(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId,
            @Parameter(description = "추방 대상 멤버 ID", required = true) Long groupMemberId
    );

    @Operation(summary = "그룹 탈퇴", description = "로그인한 사용자가 소속된 그룹에서 스스로 탈퇴합니다.")
    @ApiResponse(responseCode = "204", description = "그룹 탈퇴 성공")
    @ApiResponse(responseCode = "400", description = "SuperManager는 권한을 양도하기 전 탈퇴할 수 없음")
    @ApiResponse(responseCode = "404", description = "소속 그룹을 찾을 수 없음")
    ResponseEntity<Void> deleteGroupMemberAll(
            @Parameter(description = "로그인 유저 ID", required = true) Long userId,
            @Parameter(description = "그룹 ID", required = true) Long groupId
    );
}
