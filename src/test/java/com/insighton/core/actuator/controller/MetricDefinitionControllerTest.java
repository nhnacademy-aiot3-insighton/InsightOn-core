package com.insighton.core.actuator.controller;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetricDefinitionController.class)
class MetricDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private SensorAttributeService attributeService;

    @Test
    @DisplayName("전체 메트릭 정의 조회 성공")
    void 목록조회_성공() throws Exception {
        given(attributeService.getAllMetricDefinitions())
                .willReturn(List.of(new MetricDefinitionResponse("co2", "이산화탄소", "ppm")));

        mockMvc.perform(get("/internal/v1/metric-definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metricKey").value("co2"));
    }

    @Test
    @DisplayName("메트릭 정의 등록 성공")
    void 등록_성공() throws Exception {
        MetricDefinitionCreateRequest request = new MetricDefinitionCreateRequest("pm2.5", "미세먼지", "㎍/㎥");

        mockMvc.perform(post("/internal/v1/metric-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("이미 존재하는 metricKey면 409")
    void 등록_중복_409() throws Exception {
        willThrow(new MetricKeyAlreadyExistsException("co2"))
                .given(attributeService).createMetricDefinition(any());

        MetricDefinitionCreateRequest request = new MetricDefinitionCreateRequest("co2", "이산화탄소", "ppm");

        mockMvc.perform(post("/internal/v1/metric-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("필수값 누락이면 400")
    void 등록_필수값누락_400() throws Exception {
        String invalidJson = "{\"metricKey\":\"\",\"metricName\":\"\",\"unit\":\"ppm\"}";

        mockMvc.perform(post("/internal/v1/metric-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}