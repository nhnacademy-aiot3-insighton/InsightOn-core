package com.insighton.core.controller.groups;

import com.insighton.core.dto.groupmember.request.GroupMembersJoinRequest;
import com.insighton.core.service.GroupManagementUseCase;
import com.insighton.core.service.groupmember.GroupMembersService;
import com.insighton.core.dto.groups.response.GroupsResponse;
import com.insighton.core.service.groups.GroupsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GroupsController {
    private final GroupsService groupService;
    private final GroupMembersService groupMembersService;
    private final GroupManagementUseCase groupsUseCase;

    /**
     * Auth 서비스에서 호출하는 내부 그룹 가입 API
     * @return 성공 시 상태 200 반환
     */
    @PostMapping("/internal/groups/join-by-token")
    public ResponseEntity<Void> joinGroupByToken(@RequestBody GroupMembersJoinRequest request){
        // 1. inviteToken으로 그룹이 존재하는지 확인하고 가입 시키는 로직 호출
         groupsUseCase.joinGroupByToken(request);

        return ResponseEntity.ok().build();
    }

    /**
     * 관리자용 그룹 조회
     * @param userId login한 user의 ID
     * @param groupId 내가 속한 group의 ID
     * @return 토큰 정보가 포함된 Group 정보
     */
    @GetMapping("/api/groups/{group-id}/")
    public ResponseEntity<GroupsResponse> getGroupAdmin(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId){
        GroupsResponse response = groupsUseCase.getGroupAdmin(userId, groupId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 그룹 정보 조회 (Token 쪽 null)
     * @param userId login한 user의 ID
     * @param groupId 내가 속한 group의 ID
     * @return 토큰 정보가 빠진 그룹 조회 정보 반환
     */
    @GetMapping("/api/groups/{group-id}/my-group")
    public ResponseEntity<GroupsResponse> getMyGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId){
        GroupsResponse response = groupsUseCase.getMyGroup(userId, groupId);

        return ResponseEntity.ok(response);
    }

    /**
     * 내가 초대받은 회사의 정보
     * @param inviteToken 초대 토큰
     * @param userId login한 user의 ID
     * @param groupId 내가 속한 group의 ID
     * @return 초대 받은 회사의 정보 반환(token null)
     */
    @GetMapping("/api/groups/{group-id}/preview")
    public ResponseEntity<GroupsResponse> getGroup(
            @RequestHeader("X-USER-ID")Long userId,
            @PathVariable("group-id") Long groupId,
            @RequestBody String inviteToken){
        GroupsResponse response = groupService.getGroupPreview(inviteToken, userId, groupId);

        return ResponseEntity.ok(response);
    }

}
