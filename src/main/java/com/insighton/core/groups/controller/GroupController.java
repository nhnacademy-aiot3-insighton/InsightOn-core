package com.insighton.core.groups.controller;

import com.insighton.core.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.dto.response.GroupResponse;
import com.insighton.core.groups.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;
    private final GroupMembersService groupMembersService;

    /**
     * Auth 서비스에서 호출하는 내부 그룹 가입 API
     * @return 성공 시 상태 200 반환
     */
    @PostMapping("/internal/groups/{group-id}/join")
    public ResponseEntity<Void> joinGroupByToken(@RequestBody GroupMemberJoinRequest request){
        // 1. inviteToken으로 그룹이 존재하는지 확인하고 가입 시키는 로직 호출
        // groupMemberService.joinGroupByToken(request.inviteToken(), request.userId());

        return ResponseEntity.ok().build();
    }

    /**
     * 관리자용 그룹 조회
     * @param userId login한 user의 ID
     * @param groupId 내가 속한 group의 ID
     * @return 토큰 정보가 포함된 Group 정보
     */
    @GetMapping("/api/groups/{group-id}/")
    public ResponseEntity<GroupResponse> getGroupAdmin(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId){
        GroupResponse response = groupService.getGroupAdmin(userId, groupId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 그룹 정보 조회 (Token 쪽 null)
     * @param userId login한 user의 ID
     * @param groupId 내가 속한 group의 ID
     * @return 토큰 정보가 빠진 그룹 조회 정보 반환
     */
    @GetMapping("/api/groups/{group-id}/my-group")
    public ResponseEntity<GroupResponse> getMyGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId){
        GroupResponse response = groupService.getMyGroup(userId, groupId);

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
    public ResponseEntity<GroupResponse> getGroup(
            @RequestHeader("X-USER-ID")Long userId,
            @PathVariable("group-id") Long groupId,
            @RequestBody String inviteToken){
        GroupResponse response = groupService.getGroupPreview(inviteToken, userId, groupId);

        return ResponseEntity.ok(response);
    }

}
