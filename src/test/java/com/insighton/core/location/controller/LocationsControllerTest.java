package com.insighton.core.location.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.groups.service.CoreManagementUseCase;
import com.insighton.core.location.dto.request.LocationsCreateRequest;
import com.insighton.core.location.dto.request.LocationsUpdateRequest;
import com.insighton.core.location.dto.response.LocationsListResponse;
import com.insighton.core.location.dto.response.LocationsResponse;
import com.insighton.core.location.entity.Locations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

@WebMvcTest(LocationsController.class)
class LocationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CoreManagementUseCase useCase;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("Location 생성 성공 - 201 Created 반환")
        void createLocation_success() throws Exception {
            // given
            LocationsCreateRequest request = new LocationsCreateRequest("test", Locations.AutoControlMode.SUGGESTION);

            // when & then
            mockMvc.perform(post("/api/v1/groups/{group-id}/location/create", 1L)
                            .header("X-USER-ID", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(useCase).createLocation(eq(1L), eq(1L), any(LocationsCreateRequest.class));
        }

        @Test
        @DisplayName("Location 목록 조회 성공 - 200 OK 반환")
        void getLocationList_success() throws Exception {
            // given
            LocationsListResponse mockResponse = new LocationsListResponse(1L, "test", Locations.AutoControlMode.SUGGESTION);
            given(useCase.getLocationList(1L, 1L)).willReturn(List.of(mockResponse));

            // when & then
            mockMvc.perform(get("/api/v1/groups/{group-id}/locations", 1L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].locationId").value(1L))
                    .andExpect(jsonPath("$[0].locationName").value("test"));

            verify(useCase).getLocationList(1L, 1L);
        }

        @Test
        @DisplayName("Location 상세 정보 조회 성공 - 200 OK 반환")
        void getLocation_success() throws Exception {
            // given
            LocationsResponse mockResponse = new LocationsResponse(100L, 1L, "test", OffsetDateTime.now(), null);
            given(useCase.getLocation(1L, 1L, 100L)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/groups/{group-id}/location/{location-id}", 1L, 100L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locationId").value(100L))
                    .andExpect(jsonPath("$.locationName").value("test"));

            verify(useCase).getLocation(1L, 1L, 100L);
        }

        @Test
        @DisplayName("AutoControlMode 스위칭 성공 - 200 OK 반환")
        void toggleAutoControlMode_success() throws Exception {
            // when & then
            mockMvc.perform(put("/api/v1/groups/{group-id}/location/{location-id}/toggle-mode", 1L, 100L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isOk());

            verify(useCase).toggleAutoControlMode(1L, 1L, 100L);
        }

        @Test
        @DisplayName("Location 이름 수정 성공 - 200 OK 반환")
        void updateName_success() throws Exception {
            // given
            LocationsUpdateRequest request = new LocationsUpdateRequest("안방");

            // when & then
            mockMvc.perform(put("/api/v1/groups/{group-id}/location/{location-id}/update", 1L, 100L)
                            .header("X-USER-ID", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(useCase).updateName(eq(1L), eq(1L), eq(100L), any(LocationsUpdateRequest.class));
        }

        @Test
        @DisplayName("Location 삭제 성공 - 204 No Content 반환")
        void deleteLocation_success() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/groups/{group-id}/location/{location-id}/delete", 1L, 100L)
                            .header("X-USER-ID", 1L))
                    .andExpect(status().isNoContent());

            verify(useCase).deleteLocation(1L, 1L, 100L);
        }
    }

    @Nested
    @DisplayName("실패 및 예외 케이스")
    class FailureCases {

        @Test
        @DisplayName("필수 헤더(X-USER-ID) 누락 시 400 Bad Request")
        void missingHeader_returnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/groups/{group-id}/locations", 1L))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Location 생성 시 RequestBody 유효성 검사(@Valid) 실패 시 400 Bad Request")
        void createLocation_invalidBody_returnsBadRequest() throws Exception {
            // DTO 검증 규칙(@NotBlank 등)에 걸리는 잘못된 객체 (빈 이름)
            LocationsCreateRequest invalidRequest = new LocationsCreateRequest("", Locations.AutoControlMode.SUGGESTION);

            mockMvc.perform(post("/api/v1/groups/{group-id}/location/create", 1L)
                            .header("X-USER-ID", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Location 수정 시 RequestBody 유효성 검사(@Valid) 실패 시 400 Bad Request")
        void updateName_invalidBody_returnsBadRequest() throws Exception {
            LocationsUpdateRequest invalidRequest = new LocationsUpdateRequest(null);

            mockMvc.perform(put("/api/v1/groups/{group-id}/location/{location-id}/update", 1L, 100L)
                            .header("X-USER-ID", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }
}