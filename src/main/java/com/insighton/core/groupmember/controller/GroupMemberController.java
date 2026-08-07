package com.insighton.core.groupmember.controller;

import com.insighton.core.groupmember.dto.response.GroupMemberListResponse;
import com.insighton.core.groupmember.dto.response.GroupMemberResponse;
import com.insighton.core.groupmember.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups/{group-id}/members")
public class GroupMemberController {
    private final GroupMemberService groupMemberService;

    /**
     * 그룹 멤버 리스트 조회(관리자용(?))
     *
     * @param userId  관리자 User ID
     * @param groupId 조회하려는 group의 ID
     * @return groupMemberList 반환
     */
    @GetMapping
    public ResponseEntity<List<GroupMemberListResponse>> getGroupMemberList(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId) {

        List<GroupMemberListResponse> groupMemberListResponse = groupMemberService.getGroupMemberList(userId, groupId);

        return ResponseEntity.ok(groupMemberListResponse);
    }

    /**
     * 멤버 상세 조회
     *
     * @param userId        조회를 시도하는 user의 ID
     * @param groupId       조회하려는 member가 group의 ID
     * @param groupMemberId 조회하려는 member의 ID
     * @return member의 상세 정보 반환
     */
    @GetMapping("/{group-member-id}")
    public ResponseEntity<GroupMemberResponse> getGroupMember(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("group-member-id") Long groupMemberId
    ) {
        GroupMemberResponse groupMember = groupMemberService.getGroupMember(userId, groupId, groupMemberId);

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
    @PutMapping("/{group-member-id}/toggle-manager")
    public ResponseEntity<Void> toggleManagerRole(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("group-member-id") Long groupMemberId) {

        groupMemberService.toggleManagerRole(groupId, groupMemberId, userId);

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
    @PutMapping("/{group-member-id}/toggle-super-manager")
    public ResponseEntity<Void> toggleSuperManagerRole(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("group-member-id") Long groupMemberId) {

        groupMemberService.toggleSuperManagerRole(groupId, groupMemberId, userId);

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
    @DeleteMapping("/{group-member-id}/kick-member")
    public ResponseEntity<Void> kickGroupMember(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId,
            @PathVariable("group-member-id") Long groupMemberId) {

        groupMemberService.kickGroupMember(userId, groupId, groupMemberId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 그룹 탈퇴
     *
     * @param userId  로그인한 user의 ID
     * @param groupId 떠나려는 group의 ID
     * @return 성공 시 상태 204 반환
     */
    @DeleteMapping("/leave-group")
    public ResponseEntity<Void> deleteGroupMemberAll(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable("group-id") Long groupId) {

        groupMemberService.leaveGroup(groupId, userId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
