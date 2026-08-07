package com.insighton.core.sensor_attributes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.insighton.core.sensorattributes.controller.SensorAttributeController;
import com.insighton.core.sensorattributes.dto.SensorAttributeResponse;
import com.insighton.core.sensorattributes.service.SensorAttributeService;
import com.insighton.core.sensors.exception.SensorNotFoundException;
import org.junit.jupiter.api.Disabled;
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

@Disabled
@WebMvcTest(SensorAttributeController.class)
class SensorAttributeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private SensorAttributeService attributeService;

    // FIXME: sensor_attributes의 current_value_str / group_id 컬럼 제거로
    // SensorAttributeResponse(4-arg) / updateActuatorValue / deleteByGroupIdGroupId 가 사라져 컴파일 불가.
    // 액추에이터 제어가 actuators 테이블 기준으로 재설계되면 다시 살릴 것.
//    void 속성목록_조회_성공() throws Exception {
//        given(attributeService.getAllAttributeBySensorId(42L, 10L))
//                .willReturn(List.of(new SensorAttributeResponse("co2", "이산화탄소", "ppm", "850")));
//
//        mockMvc.perform(get("/api/v1/sensor/{sensorId}/attribute", 10L)
//                        .header("X-USER-ID", 42L))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].metricKey").value("co2"));
//    }

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
    @DisplayName("메트릭 정의 목록 조회 성공")
    void 메트릭정의_조회_성공() throws Exception {
        mockMvc.perform(get("/api/v1/sensor/{sensor-id}/attribute/definitions", 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // FIXME: sensor_attributes의 current_value_str / group_id 컬럼 제거로
    // SensorAttributeResponse(4-arg) / updateActuatorValue / deleteByGroupIdGroupId 가 사라져 컴파일 불가.
    // 액추에이터 제어가 actuators 테이블 기준으로 재설계되면 다시 살릴 것.
//    void 값변경_빈값_400() throws Exception {
//        ActuatorUpdateRequest request = new ActuatorUpdateRequest("");
//
//        mockMvc.perform(put("/api/v1/sensor/{sensor-id}/attribute/{metric-key}", 1L, "power_status")
//                        .header("X-USER-ID", 1L)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }

    // FIXME: sensor_attributes의 current_value_str / group_id 컬럼 제거로
    // SensorAttributeResponse(4-arg) / updateActuatorValue / deleteByGroupIdGroupId 가 사라져 컴파일 불가.
    // 액추에이터 제어가 actuators 테이블 기준으로 재설계되면 다시 살릴 것.
//    void 값변경_메트릭키없음_404() throws Exception {
//        willThrow(new MetricKeyNotFoundException("등록되지 않은 메트릭 키입니다"))
//                .given(attributeService)
//                .updateActuatorValue(anyLong(), anyLong(), anyString(), anyString());
//
//        ActuatorUpdateRequest request = new ActuatorUpdateRequest("ON");
//
//        mockMvc.perform(put("/api/v1/sensor/{sensor-id}/attribute/{metric-key}", 1L, "unknown_key")
//                        .header("X-USER-ID", 1L)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isNotFound());
//    }
}