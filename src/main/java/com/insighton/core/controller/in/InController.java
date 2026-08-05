package com.insighton.core.controller.in;

import com.insighton.core.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.groupmember.dto.response.GroupMemberResponse;
import com.insighton.core.groupmember.service.GroupMemberService;
import com.insighton.core.groups.service.CoreManagementUseCase;
import com.insighton.core.location.dto.response.LocationResponse;
import com.insighton.core.location.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1")
public class InController {
    private final CoreManagementUseCase coreUseCase;
    private final GroupMemberService groupMemberService;
    private final LocationService locationService;

    /**
     * Auth 서비스에서 호출하는 내부 그룹 가입 API
     *
     * @return 성공 시 상태 200 반환
     */
    @PostMapping("/groups/join-by-token")
    public ResponseEntity<Void> joinGroupByToken(
            @Valid @RequestBody GroupMemberJoinRequest request) {
        // inviteToken으로 그룹이 존재하는지 확인하고 가입 시키는 로직 호출
        coreUseCase.joinGroupByToken(request);

        return ResponseEntity.ok().build();
    }

    /**
     * 그룹 소속 확인용 내부 API — 서비스 간 호출 전용
     *
     * @return 멤버면 groupId/groupRole 포함 응답, 아니면 404
     */
    @GetMapping("groups/{group-id}/members")
    public ResponseEntity<GroupMemberResponse> getGroupMemberByUserId(
            @PathVariable("group-id") Long groupId,
            @RequestParam("user-id") Long userId
    ) {
        GroupMemberResponse response = groupMemberService.getGroupMemberAI(userId, groupId);

        return ResponseEntity.ok(response);
    }

    /**
     * 요청자가 그 location이 속한 그룹의 멤버인지 검증하기 위한 groupId 조회
     *
     * @return 성공 시 상태 200과 response 반환, 실패 시 404 반환
     */
    @GetMapping("/locations/{location-id}")
    public ResponseEntity<LocationResponse> getLocationResponse(
            @PathVariable("location-id") Long locationId
    ) {
        LocationResponse locationResponse = locationService.getLocationAI(locationId);

        return ResponseEntity.ok(locationResponse);
    }
}
