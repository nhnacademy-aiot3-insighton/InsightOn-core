package com.insighton.core.groupmember.controller;

import com.insighton.core.groupmember.dto.response.GroupMembersListResponse;
import com.insighton.core.groupmember.dto.response.GroupMembersResponse;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.service.GroupManagementUseCase;
import feign.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GroupMembersController {
    private final GroupManagementUseCase useCase;
    private final GroupMembersService groupMembersService;

    /**
     * 그룹 멤버 리스트 조회(관리자용(?))
     * @param userId 관리자 User ID
     * @param groupId 조회하려는 group의 ID
     * @return groupMemberList 반환
     */
    @GetMapping("/api/groups/{group-id}/members")
    public ResponseEntity<List<GroupMembersListResponse>> getGroupMemberList(
            @RequestHeader("X-USER-ID")Long userId,
            @PathVariable("group-id") Long groupId){

        List<GroupMembersListResponse> groupMembersListResponses = groupMembersService.getGroupMemberList(userId, groupId);

        return ResponseEntity.ok(groupMembersListResponses);
    }

    /**
     * 멤버 상세 조회
     * @param userId 조회를 시도하는 user의 ID
     * @param groupId 조회하려는 member가 group의 ID
     * @param groupMemberId 조회하려는 member의 ID
     * @return member의 상세 정보 반환
     */
    @GetMapping("/api/groups/{group-id}/members/{group-member-id}")
    public ResponseEntity<GroupMembersResponse> getGroupMember(
            @RequestHeader("X-USER-ID")Long userId,
            @PathVariable("group-id")Long groupId,
            @PathVariable("group-member-id")Long groupMemberId
    ){
        GroupMembersResponse groupMember = groupMembersService.getGroupMember(userId, groupId, groupMemberId);

        return ResponseEntity.ok(groupMember);
    }


    /**
     * 그룹 멤버 권한 변경
     * @param userId 변경을 시도하는 user의 ID
     * @param groupId 변경하려는 member가 속한 group의 ID
     * @param groupMemberId 변경 타겟인 member의 ID
     * @return 성공 시 상태 200 반환
     */
    @PutMapping("/api/groups/{group-id}/members/{group-member-id}/role-change")
    public ResponseEntity<Void> toggleManagerRole(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("group-member-id") Long groupMemberId){

        groupMembersService.toggleManagerRole(groupId, groupMemberId, userId);

        return ResponseEntity.ok().build();
    }

    /**
     * 관리자가 멤버를 추방
     * @param userId 관리자 ID
     * @param groupId 추방하려는 member가 속한 group ID
     * @param groupMemberId 추방하려는 member의 ID
     * @return 성공 시 상태 204 반환
     */
    @DeleteMapping("/api/groups/{group-id}/members/{group-member-id}/kick-member")
    public ResponseEntity<Void> kickGroupMember(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("group-member-id") Long groupMemberId){

        groupMembersService.kickGroupMember(userId, groupId, groupMemberId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 그룹 탈퇴
     * @param userId 로그인한 user의 ID
     * @param groupId 떠나려는 group의 ID
     * @return 성공 시 상태 204 반환
     */
    @DeleteMapping("/api/groups/{group-id}/members/leave-group")
    public ResponseEntity<Void> deleteGroupMemberAll(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId){

        groupMembersService.leaveGroup(groupId, userId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
