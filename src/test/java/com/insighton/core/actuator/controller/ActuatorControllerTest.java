package com.insighton.core.actuator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.controller.api.ActuatorController;
import com.insighton.core.domain.actuatorrunlogs.dto.ActuatorRunLogResponse;
import com.insighton.core.domain.actuatorrunlogs.entity.CommandType;
import com.insighton.core.domain.actuatorrunlogs.entity.ExecutedByType;
import com.insighton.core.domain.actuators.dto.ActuatorNameUpdateRequest;
import com.insighton.core.domain.actuators.dto.ActuatorRequest;
import com.insighton.core.domain.actuators.dto.ActuatorResponse;
import com.insighton.core.domain.actuators.entity.ActuatorType;
import com.insighton.core.domain.actuators.exception.ActuatorNotFoundException;
import com.insighton.core.usecase.actuator.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActuatorController.class)
class ActuatorControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateActuatorUseCase createActuatorUseCase;
    @MockitoBean
    private GetActuatorUseCase getActuatorUseCase;
    @MockitoBean
    private GetActuatorsByLocationUseCase getActuatorsByLocationUseCase;
    @MockitoBean
    private UpdateActuatorStateUseCase updateActuatorStateUseCase;
    @MockitoBean
    private UpdateActuatorNameUseCase updateActuatorNameUseCase;
    @MockitoBean
    private GetActuatorRunLogsUseCase getActuatorRunLogsUseCase;
    @MockitoBean
    private DeleteActuatorUseCase deleteActuatorUseCase;
    @MockitoBean
    private DeleteAllActuatorUseCase deleteAllActuatorUseCase;

    @Test
    @DisplayName("액추에이터 생성 성공")
    void 생성_성공() throws Exception {
        ActuatorRequest request = new ActuatorRequest(20L, "에어컨", ActuatorType.AIRCON, Map.of("power", "OFF"), null);
        given(createActuatorUseCase.createActuator(eq(1L), eq(10L), any())).willReturn(100L);

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
        given(getActuatorUseCase.getActuatorById(anyLong(), anyLong(), anyLong()))
                .willThrow(new ActuatorNotFoundException(999L));

        mockMvc.perform(get("/api/v1/groups/{group-id}/actuators/{id}", 10L, 999L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("위치별 액추에이터 목록 조회 성공")
    void 위치별조회_성공() throws Exception {
        ActuatorResponse actuator = new ActuatorResponse(1L, 20L, "에어컨", ActuatorType.AIRCON,
                Map.of("power", "ON"), OffsetDateTime.now(), OffsetDateTime.now(), null, null,
                Map.of("mode", java.util.List.of("COOL", "AUTO")));
        given(getActuatorsByLocationUseCase.getActuatorsByLocationId(1L, 10L, 20L))
                .willReturn(List.of(actuator));

        mockMvc.perform(get("/api/v1/groups/{group-id}/actuators/location/{location-id}", 10L, 20L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].actuatorId").value(1L));
    }

    @Test
    @DisplayName("상태 변경 성공")
    void 상태변경_성공() throws Exception {
        mockMvc.perform(put("/api/v1/groups/{group-id}/actuators/{id}/state", 10L, 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"power\":\"ON\"}"))
                .andExpect(status().isOk());

        // ExecutedByType은 UseCase 내부에서 항상 USER로 고정되므로 파라미터에서 빠짐
        verify(updateActuatorStateUseCase).updateActuatorState(eq(1L), eq(10L), eq(1L), anyMap());
    }

    @Test
    @DisplayName("이름 수정 성공")
    void 이름수정_성공() throws Exception {
        ActuatorNameUpdateRequest request = new ActuatorNameUpdateRequest("새 이름");

        mockMvc.perform(put("/api/v1/groups/{group-id}/actuators/{id}/name", 10L, 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent()); // 지난번에 200 -> 204로 바꾼 부분

        verify(updateActuatorNameUseCase).updateActuatorName(1L, 10L, 1L, "새 이름");
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
    @DisplayName("실행 이력 조회 성공")
    void 실행이력조회_성공() throws Exception {
        ActuatorRunLogResponse log = new ActuatorRunLogResponse(1L, 1L, CommandType.POWER_STATUS, "ON",
                ExecutedByType.USER, 1L, OffsetDateTime.now());
        given(getActuatorRunLogsUseCase.getActuatorRunLogs(eq(1L), eq(10L), eq(1L), any()))
                .willReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/groups/{group-id}/actuators/{id}/logs", 10L, 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].runLogId").value(1L));
    }

    @Test
    @DisplayName("단일 삭제 성공")
    void 삭제_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/{group-id}/actuators/{id}", 10L, 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNoContent());

        verify(deleteActuatorUseCase).deleteActuatorById(1L, 10L, 1L);
    }

    @Test
    @DisplayName("그룹 전체 삭제 성공")
    void 전체삭제_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/{group-id}/actuators", 10L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNoContent());

        verify(deleteAllActuatorUseCase).deleteAll(1L, 10L);
    }
}
