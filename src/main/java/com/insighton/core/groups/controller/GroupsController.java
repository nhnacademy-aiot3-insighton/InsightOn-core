package com.insighton.core.groups.controller;

import com.insighton.core.groupmember.dto.request.GroupMembersJoinRequest;
import com.insighton.core.groups.dto.request.GroupsCreateRequest;
import com.insighton.core.groups.dto.request.GroupsUpdateRequest;
import com.insighton.core.groups.dto.response.GroupsListResponse;
import com.insighton.core.groups.service.GroupManagementUseCase;
import com.insighton.core.groups.dto.response.GroupsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GroupsController {
    private final GroupManagementUseCase groupsUseCase;

    /**
     * Auth 서비스에서 호출하는 내부 그룹 가입 API
     * @return 성공 시 상태 200 반환
     */
    @PostMapping("/internal/groups/join-by-token")
    public ResponseEntity<Void> joinGroupByToken(@RequestBody GroupMembersJoinRequest request){
        // inviteToken으로 그룹이 존재하는지 확인하고 가입 시키는 로직 호출
         groupsUseCase.joinGroupByToken(request);

        return ResponseEntity.ok().build();
    }

    /**
     * 그룹 생성
     * @param userId login한 user의 ID
     * @param groupsCreateRequest 그룹 생성 요청 정보
     * @return 성공시 상태 201 반환
     */
    @PostMapping("/api/groups/create")
    public ResponseEntity<Void> createGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody GroupsCreateRequest groupsCreateRequest){

        groupsUseCase.createGroup(groupsCreateRequest, userId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
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
        GroupsResponse response = groupsUseCase.getGroupPreview(inviteToken, userId, groupId);

        return ResponseEntity.ok(response);
    }

    /**
     * 시스템 관리자용 그룹 리스트 조회
     * @param userRole login한 user의 role
     * @param userId 시스템 관리자의 ID
     * @return 시스템에 생성된 그룹들 리스트
     */
    @GetMapping("/api/groups/admin/group-list")
    public ResponseEntity<List<GroupsListResponse>> getGroupList(
            @RequestHeader("X-USER-ROLE") String userRole,
            @RequestHeader("X-USER-ID") Long userId){
        List<GroupsListResponse> groupsListResponses = groupsUseCase.getGroupList(userRole, userId);

        return ResponseEntity.ok(groupsListResponses);
    }


    /**
     * 토큰 재발급
     * @param userId 재발급 하려는 user의 ID
     * @param groupId 재발급 하려는 group의 ID
     * @return 성공시 상태 200 반환(?)
     */
    @PostMapping("/api/groups/{group-id}/invite-token/new")
    public ResponseEntity<Void> newInviteToken(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId){
        groupsUseCase.newInviteToken(userId, groupId);

        return ResponseEntity.ok().build();
    }


    /**
     * 그룹 수정
     * @param userId login한 user의 ID
     * @param groupId 수정하려는 Group의 ID
     * @param request group 수정 요청 정보
     * @return 성공 시 상태 200 반환
     */
    @PutMapping("/api/groups/{group-id}/update")
    public ResponseEntity<Void> updateGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @RequestBody GroupsUpdateRequest request){
        groupsUseCase.updateGroup(request, userId, groupId);

        return ResponseEntity.ok().build();
    }

    /**
     * 그룹 삭제
     * @param groupId 삭제할 그룹의 ID
     * @param userId 삭제할 권한을 가진 user ID
     * @return 성공 시 상태 204 반환
     */
    @DeleteMapping("/api/groups/{group-id}/delete")
    public ResponseEntity<Void> deleteGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId){
        groupsUseCase.deleteGroup(userId, groupId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
