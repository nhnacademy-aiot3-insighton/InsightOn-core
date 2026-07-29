package com.insighton.core.sensors.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.sensors.dto.DeviceLocationUpdateRequest;
import com.insighton.core.sensors.entity.DeviceEntity;
import com.insighton.core.sensors.entity.DeviceType;
import com.insighton.core.sensors.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DeviceRepository deviceRepository;

    @BeforeEach
    void clean() {
        deviceRepository.deleteAll();
    }

    @Test
    @DisplayName("1. 장치 검색 성공")
    void search() throws Exception {
        deviceRepository.save(
                DeviceEntity.builder()
                        .groupId(1L)
                        .deviceType(DeviceType.SENSOR)
                        .deviceName("센서A")
                        .deviceEui("EUI-3")
                        .gatewaysId(100L)
                        .locationsId(1L)
                        .createdAt(OffsetDateTime.now())
                        .build()
        );

        mockMvc.perform(get("/api/v1/sensor/search").param("eui", "EUI-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceName").value("센서A"));
    }

    @Test
    @DisplayName("2. 장치 위치 수정 성공")
    void update() throws Exception {
        DeviceEntity saved = deviceRepository.save(
                DeviceEntity.builder()
                        .groupId(1L)
                        .deviceType(DeviceType.SENSOR)
                        .deviceName("센서B")
                        .deviceEui("EUI-4")
                        .gatewaysId(100L)
                        .locationsId(1L)
                        .createdAt(OffsetDateTime.now())
                        .build()
        );

        DeviceLocationUpdateRequest req = new DeviceLocationUpdateRequest(99L);

        mockMvc.perform(put("/api/v1/sensor/" + saved.getDeviceId() + "/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("3. 단일 기기 조회 성공")
    void getDevice() throws Exception {
        DeviceEntity saved = deviceRepository.save(
                DeviceEntity.builder()
                        .groupId(1L)
                        .deviceType(DeviceType.SENSOR)
                        .deviceName("센서C")
                        .deviceEui("EUI-5")
                        .gatewaysId(100L)
                        .locationsId(1L)
                        .createdAt(OffsetDateTime.now())
                        .build()
        );

        mockMvc.perform(get("/api/v1/sensor/" + saved.getDeviceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceName").value("센서C"));
    }

    @Test
    @DisplayName("4. 존재하지 않는 기기 조회 시 404")
    void getDevice_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/sensor/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5. 장치 삭제 성공")
    void deleteDevice() throws Exception {
        DeviceEntity saved = deviceRepository.save(
                DeviceEntity.builder()
                        .groupId(1L)
                        .deviceType(DeviceType.SENSOR)
                        .deviceName("센서D")
                        .deviceEui("EUI-6")
                        .gatewaysId(100L)
                        .locationsId(1L)
                        .createdAt(OffsetDateTime.now())
                        .build()
        );

        // 💡 컨트롤러의 @DeleteMapping("/{id}/delete") 경로와 맞추어 /delete 추가
        mockMvc.perform(delete("/api/v1/sensor/" + saved.getDeviceId() + "/delete"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("6. 전체 삭제 성공")
    void deleteAllDevices() throws Exception {
        deviceRepository.save(DeviceEntity.builder()
                .groupId(1L).deviceType(DeviceType.SENSOR).deviceName("센서E")
                .deviceEui("EUI-7").gatewaysId(100L).locationsId(1L)
                .createdAt(OffsetDateTime.now())
                .build());

        deviceRepository.save(DeviceEntity.builder()
                .groupId(1L).deviceType(DeviceType.SENSOR).deviceName("센서F")
                .deviceEui("EUI-8").gatewaysId(100L).locationsId(1L)
                .createdAt(OffsetDateTime.now())
                .build());

        // 💡 컨트롤러의 @DeleteMapping("/deleteAll") 경로와 맞추어 /deleteAll 추가
        mockMvc.perform(delete("/api/v1/sensor/deleteAll"))
                .andExpect(status().isNoContent());

        assertThat(deviceRepository.count()).isEqualTo(0);
    }
}