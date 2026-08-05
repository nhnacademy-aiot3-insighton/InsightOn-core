package com.insighton.core.device_attributes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.device_attributes.dto.ActuatorUpdateRequest;
import com.insighton.core.device_attributes.dto.DeviceAttributeResponse;
import com.insighton.core.device_attributes.exception.MetricKeyNotFoundException;
import com.insighton.core.device_attributes.service.DeviceAttributeService;
import com.insighton.core.sensors.exception.DeviceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceAttributeController.class)
class DeviceAttributeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private DeviceAttributeService attributeService;

    @Test
    @DisplayName("기기 속성 전체 조회 성공")
    void 속성목록_조회_성공() throws Exception {
        given(attributeService.getAllAttributeByDeviceId(1L, 1L))
                .willReturn(List.of(new DeviceAttributeResponse("co2", "이산화탄소", "ppm", "850")));

        mockMvc.perform(get("/api/v1/sensor/{device-id}/attribute", 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metricKey").value("co2"));
    }

    @Test
    @DisplayName("존재하지 않는 디바이스 조회 시 404")
    void 속성목록_없는디바이스_404() throws Exception {
        given(attributeService.getAllAttributeByDeviceId(anyLong(), anyLong()))
                .willThrow(new DeviceNotFoundException(999L));

        mockMvc.perform(get("/api/v1/sensor/{device-id}/attribute", 999L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("메트릭 정의 목록 조회 성공")
    void 메트릭정의_조회_성공() throws Exception {
        mockMvc.perform(get("/api/v1/sensor/{device-id}/attribute/definitions", 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("빈 값으로 제어값 변경 요청 시 @Valid에 걸려 400")
    void 값변경_빈값_400() throws Exception {
        ActuatorUpdateRequest request = new ActuatorUpdateRequest("");

        mockMvc.perform(put("/api/v1/sensor/{device-id}/attribute/{metric-key}", 1L, "power_status")
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("등록되지 않은 메트릭 키면 404")
    void 값변경_메트릭키없음_404() throws Exception {
        willThrow(new MetricKeyNotFoundException("등록되지 않은 메트릭 키입니다"))
                .given(attributeService)
                .updateActuatorValue(anyLong(), anyLong(), anyString(), anyString());

        ActuatorUpdateRequest request = new ActuatorUpdateRequest("ON");

        mockMvc.perform(put("/api/v1/sensor/{device-id}/attribute/{metric-key}", 1L, "unknown_key")
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}