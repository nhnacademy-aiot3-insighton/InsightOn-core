package com.insighton.core.sensor_attributes.controller;

import com.insighton.core.controller.api.SensorAttributeController;
import com.insighton.core.domain.sensorattributes.dto.SensorAttributeResponse;
import com.insighton.core.domain.sensorattributes.exception.MetricKeyNotFoundException;
import com.insighton.core.domain.sensorattributes.service.SensorAttributeService;
import com.insighton.core.domain.sensors.exception.SensorNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SensorAttributeController.class)
class SensorAttributeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SensorAttributeService attributeService;

    @Test
    @DisplayName("센서 속성 전체 조회 성공")
    void 속성목록_조회_성공() throws Exception {
        given(attributeService.getAllAttributeBySensorId(1L, 1L))
                .willReturn(List.of(new SensorAttributeResponse("co2", "이산화탄소", "ppm")));

        mockMvc.perform(get("/api/v1/sensor/{sensor-id}/attribute", 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metricKey").value("co2"));
    }

    @Test
    @DisplayName("존재하지 않는 센서 조회 시 404")
    void 속성목록_없는센서_404() throws Exception {
        given(attributeService.getAllAttributeBySensorId(anyLong(), anyLong()))
                .willThrow(new SensorNotFoundException(999L));

        mockMvc.perform(get("/api/v1/sensor/{sensor-id}/attribute", 999L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("속성 삭제 성공")
    void 속성삭제_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/sensor/{sensor-id}/attribute/{metric-key}", 1L, "co2")
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNoContent());

        verify(attributeService).deleteAttribute(1L, 1L, "co2");
    }

    @Test
    @DisplayName("존재하지 않는 metricKey 삭제 시 404")
    void 속성삭제_없는키_404() throws Exception {
        willThrow(new MetricKeyNotFoundException("unknown"))
                .given(attributeService).deleteAttribute(anyLong(), anyLong(), anyString());

        mockMvc.perform(delete("/api/v1/sensor/{sensor-id}/attribute/{metric-key}", 1L, "unknown")
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNotFound());
    }
}