package com.insighton.core.actuator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuatorrunlogs.service.ActuatorRunLogService;
import com.insighton.core.controller.api.ActuatorController;
import com.insighton.core.domain.actuators.dto.ActuatorNameUpdateRequest;
import com.insighton.core.domain.actuators.dto.ActuatorRequest;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.exception.ActuatorNotFoundException;
import com.insighton.core.domain.actuators.service.ActuatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActuatorController.class)
class ActuatorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ActuatorService actuatorService;
    @MockBean private ActuatorRunLogService actuatorRunLogService;

    @Test
    @DisplayName("액추에이터 생성 성공")
    void 생성_성공() throws Exception {
        ActuatorRequest request = new ActuatorRequest(1L, "에어컨", ActuatorType.AIRCON, Map.of("power", "OFF"));
        given(actuatorService.createActuator(eq(1L), eq(10L), any())).willReturn(100L);

        mockMvc.perform(post("/api/v1/groups/{group-id}/actuators", 10L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }

    @Test
    @DisplayName("다른 그룹 액추에이터 조회 시 404 (존재하지 않는 것처럼 처리)")
    void 조회_다른그룹_404() throws Exception {
        given(actuatorService.getActuatorById(anyLong(), anyLong(), anyLong()))
                .willThrow(new ActuatorNotFoundException(999L));

        mockMvc.perform(get("/api/v1/groups/{group-id}/actuators/{id}", 10L, 999L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("상태 변경 성공")
    void 상태변경_성공() throws Exception {
        mockMvc.perform(put("/api/v1/groups/{group-id}/actuators/{id}/state", 10L, 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"power\":\"ON\"}"))
                .andExpect(status().isOk());

        verify(actuatorService).updateActuatorState(eq(1L), eq(10L), eq(1L), anyMap(), eq(ExecutedByType.USER));
    }

    @Test
    @DisplayName("이름 수정 - 빈 값이면 @Valid에 걸려 400")
    void 이름수정_빈값_400() throws Exception {
        ActuatorNameUpdateRequest request = new ActuatorNameUpdateRequest(" ");

        mockMvc.perform(put("/api/v1/groups/{group-id}/actuators/{id}/name", 10L, 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("단일 삭제 성공")
    void 삭제_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/{group-id}/actuators/{id}", 10L, 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk());

        verify(actuatorService).deleteActuatorById(1L, 10L, 1L);
    }

    @Test
    @DisplayName("그룹 전체 삭제 성공")
    void 전체삭제_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/{group-id}/actuators", 10L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk());

        verify(actuatorService).deleteAll(1L, 10L);
    }
}