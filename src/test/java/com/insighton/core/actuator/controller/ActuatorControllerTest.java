package com.insighton.core.actuator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.actuators.controller.ActuatorsController;
import com.insighton.core.actuators.dto.ActuatorNameUpdateRequest;
import com.insighton.core.actuators.dto.ActuatorsRequest;
import com.insighton.core.actuators.entity.ActuatorType;
import com.insighton.core.actuators.exception.ActuatorNotFoundException;
import com.insighton.core.actuators.service.ActuatorsService;
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

@WebMvcTest(ActuatorsController.class)
class ActuatorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ActuatorsService actuatorsService;

    @Test
    @DisplayName("액추에이터 생성 성공")
    void 생성_성공() throws Exception {
        ActuatorsRequest request = new ActuatorsRequest(1L, "에어컨", ActuatorType.AIRCON, Map.of("power", "OFF"));
        given(actuatorsService.createActuator(eq(1L), eq(10L), any())).willReturn(100L);

        mockMvc.perform(post("/api/v1/groups/{groupsId}/actuators", 10L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }

    @Test
    @DisplayName("다른 그룹 액추에이터 조회 시 404 (존재하지 않는 것처럼 처리)")
    void 조회_다른그룹_404() throws Exception {
        given(actuatorsService.getActuatorById(anyLong(), anyLong(), anyLong()))
                .willThrow(new ActuatorNotFoundException("액추에이터를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/groups/{groupsId}/actuators/{id}", 10L, 999L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("상태 변경 성공")
    void 상태변경_성공() throws Exception {
        mockMvc.perform(put("/api/v1/groups/{groupsId}/actuators/{id}/state", 10L, 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"power\":\"ON\"}"))
                .andExpect(status().isOk());

        verify(actuatorsService).updateActuatorState(eq(1L), eq(10L), eq(1L), anyMap(), eq(false));
    }

    @Test
    @DisplayName("이름 수정 - 빈 값이면 @Valid에 걸려 400")
    void 이름수정_빈값_400() throws Exception {
        ActuatorNameUpdateRequest request = new ActuatorNameUpdateRequest(" ");

        mockMvc.perform(put("/api/v1/groups/{groupsId}/actuators/{id}/name", 10L, 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("단일 삭제 성공")
    void 삭제_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/{groupsId}/actuators/{id}", 10L, 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk());

        verify(actuatorsService).deleteActuatorById(1L, 10L, 1L);
    }

    @Test
    @DisplayName("그룹 전체 삭제 성공")
    void 전체삭제_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/{groupsId}/actuators", 10L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk());

        verify(actuatorsService).deleteAll(1L, 10L);
    }
}