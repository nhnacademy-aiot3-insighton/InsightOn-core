package com.insighton.core.groupmember.controller;

import com.insighton.core.controller.api.GroupMemberController;
import com.insighton.core.domain.groupmember.dto.response.GroupMemberListResponse;
import com.insighton.core.domain.groupmember.dto.response.GroupMemberResponse;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.usecase.group.GroupCreateUseCase;
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


@WebMvcTest(GroupMemberController.class)
class GroupMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupCreateUseCase useCase;

    @MockitoBean
    private GroupMemberService groupMemberService;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("그룹 멤버 리스트 조회 성공 - JSON 배열 및 응답 구조 검증")
        void getGroupMemberList_success() throws Exception {
            // given
            GroupMemberListResponse mockResponse = new GroupMemberListResponse(1L, GroupMember.GroupRole.MEMBER);
            List<GroupMemberListResponse> mockList = List.of(mockResponse);
            given(groupMemberService.getGroupMemberList(1L, 1L)).willReturn(mockList);

            // when & then
            mockMvc.perform(get("/api/v1/groups/{group-id}/members", 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1)); // 반환된 리스트 개수 검증

            verify(groupMemberService).getGroupMemberList(1L, 1L);
        }

        @Test
        @DisplayName("멤버 상세 조회 성공 - JSON 객체 응답 검증")
        void getGroupMember_success() throws Exception {
            // given
            GroupMemberResponse mockResponse = new GroupMemberResponse(1L, 1L, GroupMember.GroupRole.MEMBER, "testName", "010-0000-0000", OffsetDateTime.now());
            given(groupMemberService.getGroupMember(1L, 1L, 1L)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/groups/{group-id}/members/{group-member-id}", 1L, 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk());

            verify(groupMemberService).getGroupMember(1L, 1L, 1L);
        }

        @Test
        @DisplayName("그룹 멤버 권한 변경 성공")
        void toggleManagerRole_success() throws Exception {
            mockMvc.perform(put("/api/v1/groups/{group-id}/members/{group-member-id}/toggle-manager", 1L, 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk());

            verify(groupMemberService).toggleManagerRole(1L, 1L, 1L);
        }

        @Test
        @DisplayName("super manager 권한 양도 성공")
        void toggleSuperManagerRole_success() throws Exception {
            mockMvc.perform(put("/api/v1/groups/{group-id}/members/{group-member-id}/toggle-super-manager", 1L, 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk());

            verify(groupMemberService).toggleSuperManagerRole(1L, 1L, 1L);
        }

        @Test
        @DisplayName("멤버 강퇴 성공")
        void kickGroupMember_success() throws Exception {
            mockMvc.perform(delete("/api/v1/groups/{group-id}/members/{group-member-id}/kick-member", 1L, 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isNoContent());

            verify(groupMemberService).kickGroupMember(1L, 1L, 1L);
        }

        @Test
        @DisplayName("그룹 탈퇴 성공")
        void leaveGroup_success() throws Exception {
            mockMvc.perform(delete("/api/v1/groups/{group-id}/members/leave-group", 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isNoContent());

            verify(groupMemberService).leaveGroup(1L, 1L);
        }
    }

    @Nested
    @DisplayName("실패 및 예외 케이스")
    class FailureCases {

        @Test
        @DisplayName("필수 헤더(X-USER-ID) 누락 시 400 Bad Request")
        void missingHeader_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/groups/{group-id}/members", 1L))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PathVariable 타입이 바르지 않을 경우(Long이 아닌 문자열) 400 Bad Request")
        void invalidPathVariableType_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/groups/{group-id}/members", "invalid-id")
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isBadRequest());
        }
    }
}