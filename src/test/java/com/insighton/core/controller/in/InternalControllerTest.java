package com.insighton.core.controller.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.controller.internal.InternalController;
import com.insighton.core.domain.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.domain.groupmember.dto.response.GroupMemberResponse;
import com.insighton.core.domain.groupmember.dto.response.UserGroupResponse;
import com.insighton.core.domain.groupmember.entity.GroupMember;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.location.dto.response.LocationListResponse;
import com.insighton.core.domain.location.dto.response.LocationResponse;
import com.insighton.core.domain.location.entity.Location;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.usecase.group.GroupCreateUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalController.class)
class InternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GroupCreateUseCase useCase;

    @MockitoBean
    private GroupMemberService groupMemberService;

    @MockitoBean
    private LocationService locationService;

    @Test
    @DisplayName("Auth 서비스에서 호출하는 내부 그룹 가입 API 성공")
    void joinGroupByToken_success() throws Exception {
        GroupMemberJoinRequest request = new GroupMemberJoinRequest("testToken", 1L);

        mockMvc.perform(post("/internal/v1/groups/join-by-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(useCase).joinGroupByToken(any(GroupMemberJoinRequest.class));
    }

    @Test
    @DisplayName("그룹 소속 확인용 내부 API 성공 - getGroupMemberByUserId")
    void getGroupMemberByUserId_success() throws Exception {
        GroupMemberResponse response = GroupMemberResponse.builder()
                .userId(10L)
                .groupId(1L)
                .groupRole(GroupMember.GroupRole.MEMBER)
                .build();

        given(groupMemberService.getGroupMemberAI(10L, 1L)).willReturn(response);

        mockMvc.perform(get("/internal/v1/groups/1/members")
                        .param("userId", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10L))
                .andExpect(jsonPath("$.groupId").value(1L));
    }

    @Test
    @DisplayName("그룹 내 Location 목록 조회 성공 - getLocationByGroup")
    void getLocationByGroup_success() throws Exception {
        LocationListResponse response = new LocationListResponse(100L, "거실", Location.AutoControlMode.SUGGESTION);
        given(locationService.getLocationList(1L)).willReturn(List.of(response));

        mockMvc.perform(get("/internal/v1/groups/1/locations"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].locationName").value("거실"));
    }

    @Test
    @DisplayName("Location 상세 정보 조회 성공 - getLocationResponse")
    void getLocationResponse_success() throws Exception {
        LocationResponse response = LocationResponse.builder()
                .locationId(100L)
                .groupId(1L)
                .locationName("거실")
                .autoControlMode(Location.AutoControlMode.SUGGESTION)
                .build();

        given(locationService.getLocationAI(100L)).willReturn(response);

        mockMvc.perform(get("/internal/v1/locations/100"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value(100L))
                .andExpect(jsonPath("$.locationName").value("거실"));
    }

    @Test
    @DisplayName("유저 관리자 권한 확인 성공 - existsManagerGroup")
    void existsManagerGroup_success() throws Exception {
        UserGroupResponse response = new UserGroupResponse(true, "테스트 그룹");
        given(groupMemberService.userGroupAuth(10L)).willReturn(response);

        mockMvc.perform(get("/internal/v1/users/10/group"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.groupName").value("테스트 그룹"));
    }
}