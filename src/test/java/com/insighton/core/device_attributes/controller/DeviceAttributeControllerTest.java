package com.insighton.core.device_attributes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.device_attributes.dto.ActuatorUpdateRequest;
import com.insighton.core.device_attributes.entity.DeviceAttributeEntity;
import com.insighton.core.device_attributes.repository.DeviceAttributeRepository;
import com.insighton.core.devices.entity.DeviceEntity;
import com.insighton.core.devices.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeviceAttributeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceAttributeRepository attributeRepository;

    private DeviceEntity savedDevice;

    @BeforeEach
    void setUp() {
        attributeRepository.deleteAll();
        deviceRepository.deleteAll();

        // 테스트용 기본 디바이스 영속
        DeviceEntity device = DeviceEntity.builder()
                .deviceName("테스트 디바이스")
                .deviceEui("EUI-ATTR-TEST")
                .gatewaysId(100L)
                .locationsId(1L)
                .createdAt(OffsetDateTime.now())
                .build();
        savedDevice = deviceRepository.save(device);
    }

    @Test
    @DisplayName("1. 기기 속성 목록 조회 API 성공")
    void getDeviceAttribute() throws Exception {
        // Given
        attributeRepository.save(DeviceAttributeEntity.builder()
                .deviceId(savedDevice)
                .metricKey("co2")
                .currentValueStr("750")
                .build());

        // When & Then
        mockMvc.perform(get("/api/v1/devices/" + savedDevice.getDeviceId() + "/attribute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metricKey").value("co2"))
                .andExpect(jsonPath("$[0].displayName").value("이산화탄소"))
                .andExpect(jsonPath("$[0].unit").value("ppm"))
                .andExpect(jsonPath("$[0].currentValueStr").value("750"));
    }

    @Test
    @DisplayName("2. 시스템 지원 메트릭 정의 목록 조회 API 성공")
    void getMetricDefinitions() throws Exception {
        mockMvc.perform(get("/api/v1/devices/" + savedDevice.getDeviceId() + "/attribute/definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metricKey").exists())
                .andExpect(jsonPath("$[0].metricName").exists());
    }

    @Test
    @DisplayName("3. 액추에이터 속성값 변경 제어 API 성공 (204 No Content)")
    void updateActuatorValue() throws Exception {
        // Given
        attributeRepository.save(DeviceAttributeEntity.builder()
                .deviceId(savedDevice)
                .metricKey("power_status")
                .currentValueStr("OFF")
                .build());

        ActuatorUpdateRequest request = new ActuatorUpdateRequest("ON");

        // When & Then
        mockMvc.perform(patch("/api/v1/devices/" + savedDevice.getDeviceId() + "/attribute/power_status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("4. 필수 수치값 누락 시 400 Bad Request 에러")
    void updateActuatorValue_validationFail() throws Exception {
        ActuatorUpdateRequest request = new ActuatorUpdateRequest(""); // @NotBlank 실패 유도

        mockMvc.perform(patch("/api/v1/devices/" + savedDevice.getDeviceId() + "/attribute/power_status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}