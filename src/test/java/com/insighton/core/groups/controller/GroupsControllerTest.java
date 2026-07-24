package com.insighton.core.groups.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.groupmember.dto.request.GroupMembersJoinRequest;
import com.insighton.core.groups.dto.request.GroupsCreateRequest;
import com.insighton.core.groups.dto.request.GroupsUpdateRequest;
import com.insighton.core.groups.dto.response.GroupsListResponse;
import com.insighton.core.groups.dto.response.GroupsResponse;
import com.insighton.core.groups.service.GroupManagementUseCase;
import com.insighton.core.groups.service.GroupsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupsController.class)
class GroupsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GroupManagementUseCase groupsUseCase;

    @MockitoBean
    private GroupsService groupsService;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("Auth 서비스에서 호출하는 내부 그룹 가입 API 성공")
        void joinGroupByToken_success() throws Exception {
            GroupMembersJoinRequest request = new GroupMembersJoinRequest("testToken", 1L);

            mockMvc.perform(post("/internal/groups/join-by-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(groupsUseCase).joinGroupByToken(any(GroupMembersJoinRequest.class));
        }

        @Test
        @DisplayName("그룹 생성 성공")
        void createGroup_success() throws Exception {
            GroupsCreateRequest request = new GroupsCreateRequest("testName", "testDescription", "testLocation");

            mockMvc.perform(post("/api/groups/create")
                            .header("X-USER-ID", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(groupsUseCase).createGroup(any(GroupsCreateRequest.class), eq(1L));
        }

        @Test
        @DisplayName("내 그룹 정보 조회 성공 - Mock 응답 반환 확인")
        void getMyGroup_success() throws Exception {
            // given
            GroupsResponse mockResponse = new GroupsResponse(1L, "testName", "testDescription", "testLocation", "testToken", OffsetDateTime.now());
            given(groupsUseCase.getMyGroup(1L, 1L)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/groups/{group-id}/my-group", 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk());

            verify(groupsUseCase).getMyGroup(1L, 1L);
        }

        @Test
        @DisplayName("내가 초대받은 회사의 정보 조회 성공")
        void getGroupPreview_success() throws Exception {
            // given
            GroupsResponse mockResponse = new GroupsResponse(1L, "testName", "testDescription", "testLocation", "testToken", OffsetDateTime.now());
            given(groupsUseCase.getGroupPreview("token", 1L, 1L)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/groups/{group-id}/preview", 1L)
                            .header("X-USER-ID", 1L)
                            .param("inviteToken", "testToken"))
                    .andExpect(status().isOk());

            verify(groupsUseCase).getGroupPreview("testToken", 1L, 1L);
        }

        @Test
        @DisplayName("시스템 관리자용 그룹 리스트 조회 성공 - JSON 배열 반환 검증")
        void getGroupList_success() throws Exception {
            // given
            List<GroupsListResponse> mockList = List.of(new GroupsListResponse(1L, "testName", "testDescription", "testLocation"));
            given(groupsService.getGroupList("ADMIN", 1L)).willReturn(mockList);

            // when & then
            mockMvc.perform(get("/api/groups/admin/group-list")
                            .header("X-USER-ROLE", "ADMIN")
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(groupsService).getGroupList("ADMIN", 1L);
        }

        @Test
        @DisplayName("토큰 재발급 성공")
        void newInviteToken_success() throws Exception {
            mockMvc.perform(post("/api/groups/{group-id}/invite-token/new", 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk());

            verify(groupsUseCase).newInviteToken(1L, 1L);
        }

        @Test
        @DisplayName("그룹 수정 성공")
        void updateGroup_success() throws Exception {
            GroupsUpdateRequest request = new GroupsUpdateRequest("testName", "testDescription", "testLocation");

            mockMvc.perform(put("/api/groups/{group-id}/update", 1L)
                            .header("X-USER-ID", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(groupsUseCase).updateGroup(any(GroupsUpdateRequest.class), eq(1L), eq(1L));
        }

        @Test
        @DisplayName("그룹 삭제 성공")
        void deleteGroup_success() throws Exception {
            mockMvc.perform(delete("/api/groups/{group-id}/delete", 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isNoContent());

            verify(groupsUseCase).deleteGroup(1L, 1L);
        }
    }

    @Nested
    @DisplayName("실패 및 예외 케이스")
    class FailureCases {

        @Test
        @DisplayName("필수 헤더(X-USER-ID) 누락 시 400 Bad Request")
        void missingHeader_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/groups/{group-id}/my-group", 1L))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("필수 쿼리 파라미터(inviteToken) 누락 시 400 Bad Request")
        void missingRequestParam_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/groups/{group-id}/preview", 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("그룹 수정 시 @Valid 유효성 검사 실패 시 400 Bad Request")
        void updateGroup_invalidBody_returnsBadRequest() throws Exception {
            // DTO 유효성 조건(@NotBlank 등)에 걸리는 잘못된 요청 객체
            GroupsUpdateRequest invalidRequest = new GroupsUpdateRequest(null, null, null);

            mockMvc.perform(put("/api/groups/{group-id}/update", 1L)
                            .header("X-USER-ID", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }
}