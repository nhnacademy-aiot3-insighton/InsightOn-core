package com.insighton.core.actuators.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.actuators.dto.ActuatorNameUpdateRequest;
import com.insighton.core.actuators.dto.ActuatorStateUpdateRequest;
import com.insighton.core.actuators.dto.ActuatorsRequest;
import com.insighton.core.actuators.dto.ActuatorsResponse;
import com.insighton.core.actuators.entity.ActuatorType;
import com.insighton.core.actuators.service.ActuatorsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActuatorsController.class)
public class ActuatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ActuatorsService actuatorsService;

    @Test
    @DisplayName("1. 액추에이터 신규 생성 성공 (POST /api/v1/actuators)")
    void createActuator() throws Exception {
        ActuatorsRequest request = new ActuatorsRequest(
                1L, "에어컨_01", ActuatorType.AIRCON, Map.of("power", "OFF")
        );

        given(actuatorsService.createActuator(any(ActuatorsRequest.class))).willReturn(10L);

        mockMvc.perform(post("/api/v1/actuators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));
    }

    @Test
    @DisplayName("2. 필수 입력값 누락 시 생성 실패 (400 Bad Request)")
    void createActuator_validationFail() throws Exception {
        ActuatorsRequest request = new ActuatorsRequest(null, "", null, null);

        mockMvc.perform(post("/api/v1/actuators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("3. 액추에이터 단건 상세 조회 성공 (GET /api/v1/actuators/{id})")
    void getActuator() throws Exception {
        ActuatorsResponse response = new ActuatorsResponse(
                1L, 10L, "공기청정기_01", ActuatorType.AIR_PURIFIER,
                Map.of("power", "ON"), OffsetDateTime.now(), OffsetDateTime.now()
        );

        given(actuatorsService.getActuatorById(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/actuators/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceName").value("공기청정기_01"))
                .andExpect(jsonPath("$.actuatorType").value("AIR_PURIFIER"));
    }

    @Test
    @DisplayName("4. 구역별 목록 조회 성공 (GET /api/v1/actuators/location/{locationId})")
    void getActuatorsByLocation() throws Exception {
        ActuatorsResponse response = new ActuatorsResponse(
                1L, 10L, "환풍기_01", ActuatorType.VENTILATION_FAN,
                Map.of("speed", 3), OffsetDateTime.now(), OffsetDateTime.now()
        );

        given(actuatorsService.getActuatorsByLocationId(10L)).willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/actuators/location/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceName").value("환풍기_01"));
    }

    @Test
    @DisplayName("5. 액추에이터 이름 수정 성공 (PUT /api/v1/actuators/{id}/name)")
    void updateActuatorName() throws Exception {
        ActuatorNameUpdateRequest request = new ActuatorNameUpdateRequest("변경이름");

        willDoNothing().given(actuatorsService).updateActuatorName(eq(1L), any());

        mockMvc.perform(put("/api/v1/actuators/1/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(actuatorsService).updateActuatorName(1L, "변경이름");
    }

    @Test
    @DisplayName("6. 액추에이터 제어 명령(상태값) 수정 성공 (PUT /api/v1/actuators/{id}/state)")
    void updateActuatorState() throws Exception {
        ActuatorStateUpdateRequest request = new ActuatorStateUpdateRequest(Map.of("power", "ON"));

        willDoNothing().given(actuatorsService).updateActuatorState(eq(1L), any());

        mockMvc.perform(put("/api/v1/actuators/1/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(actuatorsService).updateActuatorState(eq(1L), Map.of("power", "ON"));
    }

    @Test
    @DisplayName("7. 액추에이터 개별 삭제 성공 (DELETE /api/v1/actuators/{id}/delete)")
    void deleteActuator() throws Exception {
        willDoNothing().given(actuatorsService).deleteActuatorById(1L);

        mockMvc.perform(delete("/api/v1/actuators/1/delete"))
                .andExpect(status().isNoContent());

        verify(actuatorsService).deleteActuatorById(1L);
    }

    @Test
    @DisplayName("8. 액추에이터 전체 삭제 성공 (DELETE /api/v1/actuators/deleteAll)")
    void deleteAllActuator() throws Exception {
        willDoNothing().given(actuatorsService).deleteAll();

        mockMvc.perform(delete("/api/v1/actuators/deleteAll"))
                .andExpect(status().isNoContent());

        verify(actuatorsService).deleteAll();
    }
}