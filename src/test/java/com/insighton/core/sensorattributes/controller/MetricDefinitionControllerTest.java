package com.insighton.core.sensorattributes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.controller.internal.MetricDefinitionController;
import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionCreateRequest;
import com.insighton.core.domain.sensorattributes.dto.MetricDefinitionResponse;
import com.insighton.core.domain.sensorattributes.exception.MetricKeyAlreadyExistsException;
import com.insighton.core.domain.sensorattributes.service.SensorAttributeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MetricDefinitionController.class)
class MetricDefinitionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private SensorAttributeService sensorAttributeService;

    @Test
    @DisplayName("메트릭 정의 목록 조회 성공")
    void 목록조회_성공() throws Exception {
        given(sensorAttributeService.getAllMetricDefinitions())
                .willReturn(List.of(new MetricDefinitionResponse("co2", "이산화탄소", "ppm")));

        mockMvc.perform(get("/internal/v1/metric-definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].metricKey").value("co2"));
    }

    @Test
    @DisplayName("메트릭 정의 생성 성공")
    void 생성_성공() throws Exception {
        MetricDefinitionCreateRequest request = new MetricDefinitionCreateRequest("pm2.5", "미세먼지", "㎍/㎥");

        mockMvc.perform(post("/internal/v1/metric-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(sensorAttributeService).createMetricDefinition(request);
    }

    @Test
    @DisplayName("metricKey가 빈 값이면 @Valid에 걸려 400")
    void 생성_빈키_400() throws Exception {
        MetricDefinitionCreateRequest request = new MetricDefinitionCreateRequest(" ", "미세먼지", "㎍/㎥");

        mockMvc.perform(post("/internal/v1/metric-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미 등록된 metricKey면 409")
    void 생성_중복키_409() throws Exception {
        MetricDefinitionCreateRequest request = new MetricDefinitionCreateRequest("co2", "이산화탄소", "ppm");

        willThrow(new MetricKeyAlreadyExistsException("co2"))
                .given(sensorAttributeService).createMetricDefinition(request);

        mockMvc.perform(post("/internal/v1/metric-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
