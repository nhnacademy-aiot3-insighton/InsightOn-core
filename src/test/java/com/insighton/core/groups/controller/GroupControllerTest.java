package com.insighton.core.groups.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.controller.api.GroupController;
import com.insighton.core.domain.groups.dto.request.GroupRequest;
import com.insighton.core.domain.groups.dto.request.GroupUpdateRequest;
import com.insighton.core.domain.groups.dto.response.GroupAdminResponse;
import com.insighton.core.domain.groups.dto.response.GroupResponse;
import com.insighton.core.domain.groups.service.GroupService;
import com.insighton.core.usecase.GroupUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GroupUseCase groupsUseCase;

    @MockitoBean
    private GroupService groupService;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("그룹 생성 성공")
        void createGroup_success() throws Exception {
            GroupRequest request = new GroupRequest("testName", "testDescription", "testLocation");

            mockMvc.perform(post("/api/v1/groups/create")
                            .header("X-USER-ID", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(groupsUseCase).createGroup(any(GroupRequest.class), eq(1L));
        }

        @Test
        @DisplayName("내 그룹 정보 조회 성공 - Mock 응답 반환 확인")
        void getMyGroup_success() throws Exception {
            // given
            GroupResponse mockResponse = new GroupResponse(1L, "testName", "testDescription", "testLocation", "testToken", OffsetDateTime.now());
            given(groupsUseCase.getMyGroup(1L, 1L)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/groups/{group-id}/my-group", 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk());

            verify(groupsUseCase).getMyGroup(1L, 1L);
        }

        @Test
        @DisplayName("내가 초대받은 회사의 정보 조회 성공")
        void getGroupPreview_success() throws Exception {
            // given
            GroupResponse mockResponse = new GroupResponse(1L, "testName", "testDescription", "testLocation", "testToken", OffsetDateTime.now());
            given(groupsUseCase.getGroupPreview("testToken", 1L, 1L)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/groups/{group-id}/preview", 1L)
                            .header("X-USER-ID", 1L)
                            .param("inviteToken", "testToken"))
                    .andExpect(status().isOk());

            verify(groupsUseCase).getGroupPreview("testToken", 1L, 1L);
        }

        @Test
        @DisplayName("시스템 관리자용 그룹 리스트 조회 성공")
        void getGroupList_success() throws Exception { // ⭕ 테스트 메서드 파라미터는 비워둡니다.
            List<GroupAdminResponse> content = List.of(new GroupAdminResponse(1L, "testName", "testDescription", "testLocation"));
            Page<GroupAdminResponse> mockPage = new PageImpl<>(content);

            given(groupService.getGroupList(eq("ADMIN"), eq(1L), any(Pageable.class)))
                    .willReturn(mockPage);

            mockMvc.perform(get("/api/v1/groups/admin/group-list")
                            .header("X-USER-ROLE", "ADMIN")
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));

            verify(groupService).getGroupList(eq("ADMIN"), eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("토큰 재발급 성공")
        void newInviteToken_success() throws Exception {
            mockMvc.perform(put("/api/v1/groups/{group-id}/invite-token/new", 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk());

            verify(groupsUseCase).newInviteToken(1L, 1L);
        }

        @Test
        @DisplayName("그룹 수정 성공")
        void updateGroup_success() throws Exception {
            GroupRequest request = new GroupRequest("testName", "testDescription", "testLocation");

            mockMvc.perform(put("/api/v1/groups/{group-id}/update", 1L)
                            .header("X-USER-ID", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(groupsUseCase).updateGroup(any(GroupUpdateRequest.class), eq(1L), eq(1L));
        }

        @Test
        @DisplayName("그룹 삭제 성공")
        void deleteGroup_success() throws Exception {
            mockMvc.perform(delete("/api/v1/groups/{group-id}/delete", 1L)
                            .header("X-USER-ID", 1L)
                            .content("token"))
                    .andExpect(status().isNoContent());

            verify(groupsUseCase).deleteGroup(1L, 1L, "token");
        }
    }

    @Nested
    @DisplayName("실패 및 예외 케이스")
    class FailureCases {

        @Test
        @DisplayName("필수 헤더(X-USER-ID) 누락 시 400 Bad Request")
        void missingHeader_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/groups/{group-id}/my-group", 1L))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("필수 쿼리 파라미터(inviteToken) 누락 시 400 Bad Request")
        void missingRequestParam_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/groups/{group-id}/preview", 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isBadRequest());
        }
    }
}