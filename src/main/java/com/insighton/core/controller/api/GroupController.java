package com.insighton.core.controller.api;

import com.insighton.core.domain.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.domain.groupmember.dto.response.GroupMemberResponse;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.groups.dto.request.GroupRequest;
import com.insighton.core.domain.groups.dto.response.GroupAdminResponse;
import com.insighton.core.domain.groups.dto.response.GroupResponse;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.usecase.group.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class GroupController {
    private final GroupCreateUseCase coreUseCase;
    private final GroupGetUseCase getGroupUseCase;
    private final GroupTokenUseCase groupTokenUseCase;
    private final GroupDeleteUseCase groupDeleteUseCase;
    private final GroupUpdateUseCase groupUpdateUseCase;
    private final GroupService groupService;
    private final GroupMemberService groupMemberService;

    /**
     * 그룹 생성
     *
     * @param userId              login한 user의 ID
     * @param groupsCreateRequest 그룹 생성 요청 정보
     * @return 성공시 상태 201 반환
     */
    @PostMapping("/create")
    public ResponseEntity<Void> createGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody GroupRequest groupsCreateRequest) {

        coreUseCase.createGroup(groupsCreateRequest, userId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 내 그룹 정보 조회
     *
     * @param userId  login한 user의 ID
     * @param groupId 내가 속한 group의 ID
     * @return 토큰 정보가 빠진 그룹 조회 정보 반환
     */
    @GetMapping("/{group-id}/my-group")
    public ResponseEntity<GroupResponse> getMyGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId) {
        GroupResponse response = getGroupUseCase.getMyGroup(userId, groupId);

        return ResponseEntity.ok(response);
    }

    /**
     * 내가 초대받은 회사의 정보
     *
     * @param inviteToken 초대 토큰
     * @param userId      login한 user의 ID
     * @param groupId     내가 속한 group의 ID
     * @return 초대 받은 회사의 정보 반환(token null)
     */
    @GetMapping("/{group-id}/preview")
    public ResponseEntity<GroupResponse> getGroupPreview(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @RequestParam String inviteToken) {
        GroupResponse response = getGroupUseCase.getGroupPreview(inviteToken, userId, groupId);

        return ResponseEntity.ok(response);
    }

    /**
     * 초대 토큰으로 그룹 가입 (이미 계정이 있는 로그인 유저용 — 회원가입 시 토큰을 안 넣은 경우 등)
     *
     * @param userId      login한 user의 ID
     * @param inviteToken 초대 토큰
     * @return 성공 시 상태 200 반환
     */
    @PostMapping("/join")
    public ResponseEntity<Void> joinGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam String inviteToken) {
        coreUseCase.joinGroupByToken(GroupMemberJoinRequest.builder()
                .inviteToken(inviteToken)
                .userId(userId)
                .build());

        return ResponseEntity.ok().build();
    }

    /**
     * 내 그룹 멤버십 조회 — 로그인 직후 세션에 groupId/권한 캐싱용.
     * groupId를 몰라도 X-USER-ID만으로 소속 그룹과 권한을 알아낼 수 있음.
     *
     * @param userId login한 user의 ID
     * @return 소속 그룹의 groupId/groupRole (소속 없으면 404)
     */
    @GetMapping("/my-membership")
    public ResponseEntity<GroupMemberResponse> getMyMembership(
            @RequestHeader("X-USER-ID") Long userId) {
        GroupMemberResponse response = groupMemberService.getMyGroupMember(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 시스템 관리자용 그룹 리스트 조회
     *
     * @param userRole login한 user의 role
     * @param userId   시스템 관리자의 ID
     * @return 시스템에 생성된 그룹들 리스트
     */
    @GetMapping("/admin/group-list")
    public ResponseEntity<Page<GroupAdminResponse>> getGroupList(
            @RequestHeader("X-USER-ROLE") String userRole,
            @RequestHeader("X-USER-ID") Long userId,
            @PageableDefault(size = 10, sort = "groupId") Pageable pageable) {

        Page<GroupAdminResponse> groupsListResponses = groupService.getGroupList(userRole, userId, pageable);

        return ResponseEntity.ok(groupsListResponses);
    }


    /**
     * 토큰 재발급
     *
     * @param userId  재발급 하려는 user의 ID
     * @param groupId 재발급 하려는 group의 ID
     * @return 성공시 상태 200 반환(?)
     */
    @PutMapping("/{group-id}/invite-token/new")
    public ResponseEntity<Void> newInviteToken(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId) {
        groupTokenUseCase.newInviteToken(userId, groupId);

        return ResponseEntity.ok().build();
    }


    /**
     * 그룹 수정
     *
     * @param userId  login한 user의 ID
     * @param groupId 수정하려는 Group의 ID
     * @param request group 수정 요청 정보
     * @return 성공 시 상태 200 반환
     */
    @PutMapping("/{group-id}/update")
    public ResponseEntity<Void> updateGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @Valid @RequestBody GroupRequest request) {
        groupUpdateUseCase.updateGroup(request, userId, groupId);

        return ResponseEntity.ok().build();
    }

    /**
     * 그룹 삭제
     *
     * @param groupId 삭제할 그룹의 ID
     * @param userId  삭제할 권한을 가진 user ID
     * @return 성공 시 상태 204 반환
     */
    @DeleteMapping("/{group-id}/delete")
    public ResponseEntity<Void> deleteGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @RequestParam String inviteToken) {
        groupDeleteUseCase.deleteGroup(userId, groupId, inviteToken);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
