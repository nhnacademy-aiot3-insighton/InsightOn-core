package com.insighton.core.controller.api;

import com.insighton.core.controller.swagger.GroupControllerApi;
import com.insighton.core.domain.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.domain.groupmember.dto.response.MyGroupIdResponse;
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
public class GroupController implements GroupControllerApi {
    private final GroupCreateUseCase coreUseCase;
    private final GroupGetUseCase getGroupUseCase;
    private final GroupTokenUseCase groupTokenUseCase;
    private final GroupDeleteUseCase groupDeleteUseCase;
    private final GroupUpdateUseCase groupUpdateUseCase;
    private final GroupService groupService;
    private final GroupMemberService groupMemberService;

    /**
     * 로그인한 유저가 속한 groupId 조회 (한 유저 = 한 그룹). groupId를 모르는 상태에서
     * 다른 API들을 호출하기 전에 front가 이걸로 먼저 groupId를 알아냄.
     *
     * @param userId login한 user의 ID
     * @return 소속된 groupId
     */
    @Override
    @GetMapping("/my")
    public ResponseEntity<MyGroupIdResponse> getMyGroupId(
            @RequestHeader("X-USER-ID") Long userId) {
        Long groupId = groupMemberService.getMyGroupId(userId);

        return ResponseEntity.ok(new MyGroupIdResponse(groupId));
    }

    @Override
    @PostMapping("/create")
    public ResponseEntity<Void> createGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody GroupRequest groupsCreateRequest) {

        coreUseCase.createGroup(groupsCreateRequest, userId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    @GetMapping("/{group-id}/my-group")
    public ResponseEntity<GroupResponse> getMyGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId) {
        GroupResponse response = getGroupUseCase.getMyGroup(userId, groupId);

        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{group-id}/preview")
    public ResponseEntity<GroupResponse> getGroupPreview(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @RequestParam String inviteToken) {
        GroupResponse response = getGroupUseCase.getGroupPreview(inviteToken, userId, groupId);

        return ResponseEntity.ok(response);
    }

    @Override
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

    @Override
    @GetMapping("/admin/group-list")
    public ResponseEntity<Page<GroupAdminResponse>> getGroupList(
            @RequestHeader("X-USER-ROLE") String userRole,
            @RequestHeader("X-USER-ID") Long userId,
            @PageableDefault(size = 10, sort = "groupId") Pageable pageable) {

        Page<GroupAdminResponse> groupsListResponses = groupService.getGroupList(userRole, userId, pageable);

        return ResponseEntity.ok(groupsListResponses);
    }

    @Override
    @PutMapping("/{group-id}/invite-token/new")
    public ResponseEntity<Void> newInviteToken(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId) {
        groupTokenUseCase.newInviteToken(userId, groupId);

        return ResponseEntity.ok().build();
    }

    @Override
    @PutMapping("/{group-id}/update")
    public ResponseEntity<Void> updateGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @Valid @RequestBody GroupRequest request) {
        groupUpdateUseCase.updateGroup(request, userId, groupId);

        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping("/{group-id}/delete")
    public ResponseEntity<Void> deleteGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @RequestParam String inviteToken) {
        groupDeleteUseCase.deleteGroup(userId, groupId, inviteToken);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
