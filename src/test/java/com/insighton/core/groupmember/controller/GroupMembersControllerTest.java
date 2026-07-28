package com.insighton.core.groupmember.controller;

import com.insighton.core.groupmember.dto.response.GroupMembersListResponse;
import com.insighton.core.groupmember.dto.response.GroupMembersResponse;
import com.insighton.core.groupmember.entity.GroupMembers;
import com.insighton.core.groupmember.service.GroupMembersService;
import com.insighton.core.groups.service.GroupManagementUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupMembersController.class)
class GroupMembersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupManagementUseCase useCase;

    @MockitoBean
    private GroupMembersService groupMembersService;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("그룹 멤버 리스트 조회 성공 - JSON 배열 및 응답 구조 검증")
        void getGroupMemberList_success() throws Exception {
            // given
            GroupMembersListResponse mockResponse = new GroupMembersListResponse(1L, GroupMembers.GroupRole.MEMBER);
            List<GroupMembersListResponse> mockList = List.of(mockResponse);
            given(groupMembersService.getGroupMemberList(1L, 1L)).willReturn(mockList);

            // when & then
            mockMvc.perform(get("/api/groups/{group-id}/members", 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1)); // 반환된 리스트 개수 검증

            verify(groupMembersService).getGroupMemberList(1L, 1L);
        }

        @Test
        @DisplayName("멤버 상세 조회 성공 - JSON 객체 응답 검증")
        void getGroupMember_success() throws Exception {
            // given
            GroupMembersResponse mockResponse = new GroupMembersResponse(1L, 1L, GroupMembers.GroupRole.MEMBER, "testName", "010-0000-0000", OffsetDateTime.now());
            given(groupMembersService.getGroupMember(1L, 1L, 1L)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/groups/{group-id}/members/{group-member-id}", 1L, 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk());

            verify(groupMembersService).getGroupMember(1L, 1L, 1L);
        }

        @Test
        @DisplayName("그룹 멤버 권한 변경 성공")
        void toggleManagerRole_success() throws Exception {
            mockMvc.perform(put("/api/groups/{group-id}/members/{group-member-id}/role-change", 1L, 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk());

            verify(groupMembersService).toggleManagerRole(1L, 1L, 1L);
        }

        @Test
        @DisplayName("멤버 강퇴 성공")
        void kickGroupMember_success() throws Exception {
            mockMvc.perform(delete("/api/groups/{group-id}/members/{group-member-id}/kick-member", 1L, 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isNoContent());

            verify(groupMembersService).kickGroupMember(1L, 1L, 1L);
        }

        @Test
        @DisplayName("그룹 탈퇴 성공")
        void leaveGroup_success() throws Exception {
            mockMvc.perform(delete("/api/groups/{group-id}/members/leave-group", 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isNoContent());

            verify(groupMembersService).leaveGroup(1L, 1L);
        }
    }

    @Nested
    @DisplayName("실패 및 예외 케이스")
    class FailureCases {

        @Test
        @DisplayName("필수 헤더(X-USER-ID) 누락 시 400 Bad Request")
        void missingHeader_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/groups/{group-id}/members", 1L))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PathVariable 타입이 바르지 않을 경우(Long이 아닌 문자열) 400 Bad Request")
        void invalidPathVariableType_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/groups/{group-id}/members", "invalid-id")
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isBadRequest());
        }
    }
}