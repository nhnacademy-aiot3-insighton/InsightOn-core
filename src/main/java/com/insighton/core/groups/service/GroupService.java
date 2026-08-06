package com.insighton.core.groups.service;


import com.insighton.core.groups.dto.request.GroupRequest;
import com.insighton.core.groups.dto.response.GroupAdminResponse;
import com.insighton.core.groups.dto.response.GroupResponse;
import com.insighton.core.groups.entity.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GroupService {
    /**
     * 그룹 생성
     *
     * @param request Group 생성 요청 정보
     * @return 방금 만든 group 정보를 반환한다.
     */
    Group createGroup(GroupRequest request);

    /**
     * 그룹 수정
     *
     * @param request Group 수정 요청 정보
     * @param groupId 수정하려는 group의 ID
     */
    void updateGroup(GroupRequest request, Long groupId);


    /**
     * 일반 사용자의 초대한 그룹 조회용 (초대장 느낌)
     * 이 초대 토큰은 A 회사의 초대 토큰 입니다 하고 회사 정보를 띄우기.(토큰은 null 값으로 들어감)
     *
     * @param inviteToken 초대 코드
     * @param groupId     내가 지금 보고 있는 group의 ID
     * @return 토큰 값 빼고 group 정보가 들어감
     */
    GroupResponse getGroupPreview(String inviteToken, Long groupId);


    /**
     * 시스템 관리자가 group List를 조회 / page로 조회하는 걸로 바꾸기
     *
     * @param userRole 로그인한 사용자의 권한...?
     * @param userId   login한 user의 ID
     * @return GroupList 반환
     */
    Page<GroupAdminResponse> getGroupList(String userRole, Long userId, Pageable pageable);

    /**
     * 토큰 재발급
     *
     * @param groupId 그룹 id
     */
    void newInviteToken(Long groupId);


    /**
     * 그룹 삭제
     *
     * @param groupId 그룹 id
     */
    void deleteGroup(Long groupId);

    /**
     * 초대 토큰으로 group이 존재하는지 확인 (가입용)
     *
     * @param inviteToken 초대 토큰
     */
    Group validateGroupByInviteToken(String inviteToken);

    /**
     * id로 찾아서 안되면 예외던지기
     *
     * @param groupId 찾으려는 group
     * @return group 반환
     */
    Group groupFindById(Long groupId);
}
