package com.insighton.core.service.groups;


import com.insighton.core.dto.groups.request.GroupCreateRequest;
import com.insighton.core.dto.groups.response.GroupListResponse;
import com.insighton.core.dto.groups.response.GroupResponse;

import java.util.List;

public interface GroupService {
    /**
     * 그룹 생성
     * @param request Group 생성 요청 정보
     */
    void createGroup(GroupCreateRequest request, Long userId);

    /**
     * 관리자용 group 정보 조회
     * @param userId login한 user의 ID
     * @return 그룹 정보
     */
    GroupResponse getGroupAdmin(Long userId, Long groupId);

    /**
     * 일반 사용자의 초대한 그룹 조회용 (초대장 느낌)
     * 이 초대 토큰은 A 회사의 초대 토큰 입니다 하고 회사 정보를 띄우기.(토큰은 null 값으로 들어감)
     * @param inviteToken 초대 코드
     * @param userId 로그인한 user의 ID
     * @param groupId 내가 지금 보고 있는 group의 ID
     * @return 토큰 값 빼고 group 정보가 들어감
     */
    GroupResponse getGroupPreview(String inviteToken, Long userId, Long groupId);

    /**
     * 현재 로그인한 사용자의 소속 그룹 정보 조회
     * (한 계정당 하나의 그룹만 가입 가능하므로, 로그인 정보 기반으로 해당 그룹 정보를 반환)
     * @param userId login한 user의 ID
     * @return token 정보를 제외한 group의 정보
     */
    GroupResponse getMyGroup(Long userId, Long groupId);

    /**
     * 시스템 관리자가 group List를 조회
     * @param userRole 로그인한 사용자의 권한...?
     * @param userId 로그인한 user ID
     * @return GroupList 반환
     */
    List<GroupListResponse> getGroupList(String userRole, Long userId);

    /**
     * 토큰 재발급
     * @param groupId 그룹 id
     */
    void newInviteToken(Long groupId);


    /**
     * 그룹 삭제
     * @param groupId 그룹 id
     */
    void deleteGroup(Long groupId);
}
