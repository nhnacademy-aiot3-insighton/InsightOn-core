package com.insighton.core.sensorattributes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.controller.api.SensorAttributeController;
import com.insighton.core.domain.sensorattributes.dto.SensorAttributeResponse;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import com.insighton.core.usecase.sensorattribute.DeleteAttributeUseCase;
import com.insighton.core.usecase.sensorattribute.GetAllAttributeUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensorAttributeController.class)
class SensorAttributeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private GetAllAttributeUseCase getAllAttributeUseCase;
    @MockitoBean
    private DeleteAttributeUseCase deleteAttributeUseCase;

    @Test
    @DisplayName("속성 목록 조회 성공")
    void 속성목록_조회_성공() throws Exception {
        given(getAllAttributeUseCase.getAllAttributeBySensorId(1L, 1L))
                .willReturn(List.of(new SensorAttributeResponse("co2", "이산화탄소", "ppm")));

        mockMvc.perform(get("/api/v1/sensor/{sensor-id}/attribute", 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].metricKey").value("co2"));
    }

    @Test
    @DisplayName("존재하지 않는 센서 조회 시 404")
    void 속성목록_없는센서_404() throws Exception {
        given(getAllAttributeUseCase.getAllAttributeBySensorId(anyLong(), anyLong()))
                .willThrow(new SensorNotFoundException(999L));

        mockMvc.perform(get("/api/v1/sensor/{sensor-id}/attribute", 999L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("속성 삭제 성공")
    void 속성_삭제_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/sensor/{sensor-id}/attribute/{metric-key}", 1L, "co2")
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNoContent());

        verify(deleteAttributeUseCase).deleteAttribute(1L, 1L, "co2");
    }
}
