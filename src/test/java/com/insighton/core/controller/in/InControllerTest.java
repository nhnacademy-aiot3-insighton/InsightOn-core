package com.insighton.core.controller.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.controller.internal.InternalController;
import com.insighton.core.domain.groupmember.dto.request.GroupMemberJoinRequest;
import com.insighton.core.domain.groupmember.service.GroupMemberService;
import com.insighton.core.domain.location.service.LocationService;
import com.insighton.core.usecase.GroupUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalController.class)
class InControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GroupUseCase useCase;

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
                .andDo(print()) // 실행 결과 및 에러 응답 콘솔
                .andExpect(status().isOk());

        verify(useCase).joinGroupByToken(any(GroupMemberJoinRequest.class));
    }

}