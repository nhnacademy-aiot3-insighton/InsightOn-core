package com.insighton.core.groupregistration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.controller.api.GroupRegistrationController;
import com.insighton.core.domain.groupregistration.dto.CreateGroupRegistrationRequest;
import com.insighton.core.domain.groupregistration.dto.GroupRegistrationResponse;
import com.insighton.core.domain.groupregistration.entity.GroupRegistrationStatus;
import com.insighton.core.domain.groupregistration.exception.GroupRegistrationNotFoundException;
import com.insighton.core.domain.groupregistration.exception.UnauthorizedGroupRegistrationAccessException;
import com.insighton.core.domain.groupregistration.service.GroupRegistrationService;
import com.insighton.core.usecase.GroupRegistrationApprovalUseCase;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupRegistrationController.class)
class GroupRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GroupRegistrationService groupRegistrationService;

    @MockitoBean
    private GroupRegistrationApprovalUseCase approvalUseCase;

    private static final Long GROUP_REGISTRATION_ID = 10L;
    private static final Long REQUESTER_ID = 1L;

    private GroupRegistrationResponse dummyResponse() {
        return new GroupRegistrationResponse(GROUP_REGISTRATION_ID, REQUESTER_ID, "Test Group", "Desc", "Seoul",
                GroupRegistrationStatus.PENDING, null, OffsetDateTime.now(), null);
    }

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("그룹 등록 신청 생성 성공")
        void createRequest_success() throws Exception {
            CreateGroupRegistrationRequest request = new CreateGroupRegistrationRequest("Test Group", "Desc", "Seoul");
            given(groupRegistrationService.createRequest(eq(REQUESTER_ID), any(CreateGroupRegistrationRequest.class)))
                    .willReturn(dummyResponse());

            mockMvc.perform(post("/api/v1/group-registrations")
                            .header("X-User-Id", REQUESTER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(groupRegistrationService).createRequest(eq(REQUESTER_ID), any(CreateGroupRegistrationRequest.class));
        }

        @Test
        @DisplayName("전체 신청 목록 조회 성공")
        void getGroupRegistrations_success() throws Exception {
            Page<GroupRegistrationResponse> page = new PageImpl<>(List.of(dummyResponse()));
            given(groupRegistrationService.getGroupRegistrations(eq("ADMIN"), eq(GroupRegistrationStatus.PENDING), any(Pageable.class)))
                    .willReturn(page);

            mockMvc.perform(get("/api/v1/group-registrations")
                            .header("X-User-Role", "ADMIN")
                            .param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("내 신청 목록 조회 성공")
        void getMyGroupRegistrations_success() throws Exception {
            Page<GroupRegistrationResponse> page = new PageImpl<>(List.of(dummyResponse()));
            given(groupRegistrationService.getMyGroupRegistrations(eq(REQUESTER_ID), any(Pageable.class)))
                    .willReturn(page);

            mockMvc.perform(get("/api/v1/group-registrations/my")
                            .header("X-User-Id", REQUESTER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("신청 상세 조회 성공")
        void getGroupRegistration_success() throws Exception {
            given(groupRegistrationService.getGroupRegistration(eq(GROUP_REGISTRATION_ID), eq(REQUESTER_ID), isNull()))
                    .willReturn(dummyResponse());

            mockMvc.perform(get("/api/v1/group-registrations/{group-registration-id}", GROUP_REGISTRATION_ID)
                            .header("X-User-Id", REQUESTER_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("신청 취소 성공")
        void cancelGroupRegistration_success() throws Exception {
            mockMvc.perform(put("/api/v1/group-registrations/{group-registration-id}/cancel", GROUP_REGISTRATION_ID)
                            .header("X-User-Id", REQUESTER_ID))
                    .andExpect(status().isOk());

            verify(groupRegistrationService).cancelGroupRegistration(GROUP_REGISTRATION_ID, REQUESTER_ID);
        }

        @Test
        @DisplayName("신청 승인 성공")
        void approveGroupRegistration_success() throws Exception {
            Long approverId = 99L;

            mockMvc.perform(put("/api/v1/group-registrations/{group-registration-id}/approve", GROUP_REGISTRATION_ID)
                            .header("X-User-Role", "ADMIN")
                            .header("X-User-Id", approverId))
                    .andExpect(status().isOk());

            verify(approvalUseCase).approve("ADMIN", GROUP_REGISTRATION_ID, approverId);
        }

        @Test
        @DisplayName("신청 거절 성공")
        void rejectGroupRegistration_success() throws Exception {
            Long approverId = 99L;

            mockMvc.perform(put("/api/v1/group-registrations/{group-registration-id}/reject", GROUP_REGISTRATION_ID)
                            .header("X-User-Role", "ADMIN")
                            .header("X-User-Id", approverId))
                    .andExpect(status().isOk());

            verify(groupRegistrationService).rejectGroupRegistration("ADMIN", GROUP_REGISTRATION_ID, approverId);
        }
    }

    @Nested
    @DisplayName("실패 및 예외 케이스")
    class FailureCases {

        @Test
        @DisplayName("필수 헤더(X-User-Id) 누락 시 400 Bad Request")
        void createRequest_missingHeader_returnsBadRequest() throws Exception {
            CreateGroupRegistrationRequest request = new CreateGroupRegistrationRequest("Test Group", "Desc", "Seoul");

            mockMvc.perform(post("/api/v1/group-registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("필수값 누락 시 @Valid 유효성 검사 실패 - 400 Bad Request")
        void createRequest_invalidBody_returnsBadRequest() throws Exception {
            CreateGroupRegistrationRequest invalidRequest = new CreateGroupRegistrationRequest(null, "Desc", null);

            mockMvc.perform(post("/api/v1/group-registrations")
                            .header("X-User-Id", REQUESTER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("관리자가 아닌데 승인 시도 - 403 Forbidden (GlobalExceptionHandler 매핑 확인)")
        void approveGroupRegistration_unauthorized_returnsForbidden() throws Exception {
            willThrow(new UnauthorizedGroupRegistrationAccessException())
                    .given(approvalUseCase).approve(eq("USER"), anyLong(), anyLong());

            mockMvc.perform(put("/api/v1/group-registrations/{group-registration-id}/approve", GROUP_REGISTRATION_ID)
                            .header("X-User-Role", "USER")
                            .header("X-User-Id", 99L))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("존재하지 않는 신청 조회 - 404 Not Found (GlobalExceptionHandler 매핑 확인)")
        void getGroupRegistration_notFound_returnsNotFound() throws Exception {
            given(groupRegistrationService.getGroupRegistration(eq(GROUP_REGISTRATION_ID), eq(REQUESTER_ID), isNull()))
                    .willThrow(new GroupRegistrationNotFoundException());

            mockMvc.perform(get("/api/v1/group-registrations/{group-registration-id}", GROUP_REGISTRATION_ID)
                            .header("X-User-Id", REQUESTER_ID))
                    .andExpect(status().isNotFound());
        }
    }
}
