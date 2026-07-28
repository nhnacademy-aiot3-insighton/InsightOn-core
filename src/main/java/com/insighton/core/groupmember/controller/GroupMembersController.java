package com.insighton.core.groupmember.controller;

import com.insighton.core.groupmember.dto.response.GroupMembersListResponse;
import com.insighton.core.groupmember.dto.response.GroupMembersResponse;
import com.insighton.core.groupmember.service.GroupMembersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GroupMembersController {
    private final GroupMembersService groupMembersService;

    /**
     * 그룹 소속 확인용 내부 API — 서비스 간 호출 전용
     *
     * @param groupId 그룹 ID
     * @param userId  소속 여부를 확인할 유저 ID
     * @return 멤버면 groupId/groupRole 포함 응답, 아니면 404
     */
    @GetMapping("/internal/groups/{group-id}/members/user/{user-id}")
    public ResponseEntity<GroupMembersResponse> getGroupMemberByUserId(
            @PathVariable("group-id") Long groupId,
            @PathVariable("user-id") Long userId
    ) {
        GroupMembersResponse response = groupMembersService.getGroupMemberAI(userId, groupId);

        return ResponseEntity.ok(response);
    }

    /**
     * 그룹 멤버 리스트 조회(관리자용(?))
     *
     * @param userId  관리자 User ID
     * @param groupId 조회하려는 group의 ID
     * @return groupMemberList 반환
     */
    @GetMapping("/api/v1/groups/{group-id}/members")
    public ResponseEntity<List<GroupMembersListResponse>> getGroupMemberList(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId) {

        List<GroupMembersListResponse> groupMembersListResponses = groupMembersService.getGroupMemberList(userId, groupId);

        return ResponseEntity.ok(groupMembersListResponses);
    }

    /**
     * 멤버 상세 조회
     *
     * @param userId        조회를 시도하는 user의 ID
     * @param groupId       조회하려는 member가 group의 ID
     * @param groupMemberId 조회하려는 member의 ID
     * @return member의 상세 정보 반환
     */
    @GetMapping("/api/v1/groups/{group-id}/members/{group-member-id}")
    public ResponseEntity<GroupMembersResponse> getGroupMember(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("group-member-id") Long groupMemberId
    ) {
        GroupMembersResponse groupMember = groupMembersService.getGroupMember(userId, groupId, groupMemberId);

        return ResponseEntity.ok(groupMember);
    }


    /**
     * 그룹 멤버 권한 변경
     *
     * @param userId        변경을 시도하는 user의 ID
     * @param groupId       변경하려는 member가 속한 group의 ID
     * @param groupMemberId 변경 타겟인 member의 ID
     * @return 성공 시 상태 200 반환
     */
    @PutMapping("/api/v1/groups/{group-id}/members/{group-member-id}/toggle-manager")
    public ResponseEntity<Void> toggleManagerRole(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("group-member-id") Long groupMemberId) {

        groupMembersService.toggleManagerRole(groupId, groupMemberId, userId);

        return ResponseEntity.ok().build();
    }

    /**
     * super manager가 자신의 권한을 manager에게 양도
     *
     * @param userId        super manager의 user ID
     * @param groupId       super manager와 target이 속해있는 Group ID
     * @param groupMemberId target의 Group Member ID
     * @return 성공 시 상태 200 반환
     */
    @PutMapping("/api/v1/groups/{group-id}/members/{group-member-id}/toggle-super-manager")
    public ResponseEntity<Void> toggleSuperManagerRole(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("group-member-id") Long groupMemberId) {

        groupMembersService.toggleSuperManagerRole(groupId, groupMemberId, userId);

        return ResponseEntity.ok().build();
    }

    /**
     * 관리자가 멤버를 추방
     *
     * @param userId        관리자 ID
     * @param groupId       추방하려는 member가 속한 group ID
     * @param groupMemberId 추방하려는 member의 ID
     * @return 성공 시 상태 204 반환
     */
    @DeleteMapping("/api/v1/groups/{group-id}/members/{group-member-id}/kick-member")
    public ResponseEntity<Void> kickGroupMember(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("group-member-id") Long groupMemberId) {

        groupMembersService.kickGroupMember(userId, groupId, groupMemberId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 그룹 탈퇴
     *
     * @param userId  로그인한 user의 ID
     * @param groupId 떠나려는 group의 ID
     * @return 성공 시 상태 204 반환
     */
    @DeleteMapping("/api/v1/groups/{group-id}/members/leave-group")
    public ResponseEntity<Void> deleteGroupMemberAll(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId) {

        groupMembersService.leaveGroup(groupId, userId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
