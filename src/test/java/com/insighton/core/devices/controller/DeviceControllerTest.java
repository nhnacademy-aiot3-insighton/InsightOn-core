package com.insighton.core.devices.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.devices.dto.DeviceRequest;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    DeviceRepository deviceRepository;

    @BeforeEach
    void clean() {
        deviceRepository.deleteAll();
    }

    @Test
    @DisplayName("1. 장치 등록 성공")
    void create() throws Exception {
        DeviceRequest req = new DeviceRequest("센서", "EUI-1", 100L, 1L);

        // 💡 /api/v1/devices 로 URL 수정
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. 필수값 누락 시 에러 (400 Bad Request)")
    void create_fail() throws Exception {
        DeviceRequest req = new DeviceRequest("", "EUI-2", null, 1L);

        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("3. 장치 검색 성공")
    void search() throws Exception {
        deviceRepository.save(
                DeviceEntity.builder().deviceName("센서A").deviceEui("EUI-3").gatewaysId(100L).locationsId(1L).build()
        );

        mockMvc.perform(get("/api/v1/devices/search").param("eui", "EUI-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceName").value("센서A"));
    }

    @Test
    @DisplayName("4. 장치 위치 수정 성공")
    void update() throws Exception {
        DeviceEntity saved = deviceRepository.save(
                DeviceEntity.builder().deviceName("센서B").deviceEui("EUI-4").gatewaysId(100L).locationsId(1L).build()
        );

        DeviceUpdateRequest req = new DeviceUpdateRequest("센서B", 99L);

        mockMvc.perform(patch("/api/v1/devices/" + saved.getDeviceId() + "/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("5. 단일 기기 조회 성공")
    void getDevice() throws Exception {
        DeviceEntity saved = deviceRepository.save(
                DeviceEntity.builder().deviceName("센서C").deviceEui("EUI-5").gatewaysId(100L).locationsId(1L).build()
        );

        mockMvc.perform(get("/api/v1/devices/" + saved.getDeviceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceName").value("센서C"));
    }

    @Test
    @DisplayName("6. 존재하지 않는 기기 조회 시 404")
    void getDevice_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/devices/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("7. 장치 삭제 성공")
    void deleteDevice() throws Exception {
        DeviceEntity saved = deviceRepository.save(
                DeviceEntity.builder().deviceName("센서D").deviceEui("EUI-6").gatewaysId(100L).locationsId(1L).build()
        );

        mockMvc.perform(delete("/api/v1/devices/" + saved.getDeviceId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("8. 존재하지 않는 기기 삭제 시 404")
    void deleteDevice_notFound() throws Exception {
        mockMvc.perform(delete("/api/v1/devices/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("9. 전체 삭제 성공")
    void deleteAllDevices() throws Exception {
        deviceRepository.save(DeviceEntity.builder().deviceName("센서E").deviceEui("EUI-7").gatewaysId(100L).locationsId(1L).build());
        deviceRepository.save(DeviceEntity.builder().deviceName("센서F").deviceEui("EUI-8").gatewaysId(100L).locationsId(1L).build());

        mockMvc.perform(delete("/api/v1/devices"))
                .andExpect(status().isNoContent());

        assertThat(deviceRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("10. 게이트웨이 ID로 검색 성공")
    void search_byGatewayId() throws Exception {
        deviceRepository.save(
                DeviceEntity.builder().deviceName("센서G").deviceEui("EUI-9").gatewaysId(200L).locationsId(1L).build()
        );

        mockMvc.perform(get("/api/v1/devices/search").param("gatewayId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceName").value("센서G"));
    }

    @Test
    @DisplayName("11. 이름으로 검색 성공")
    void search_byName() throws Exception {
        deviceRepository.save(
                DeviceEntity.builder().deviceName("센서H").deviceEui("EUI-10").gatewaysId(100L).locationsId(1L).build()
        );

        mockMvc.perform(get("/api/v1/devices/search").param("deviceName", "센서H"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceEui").value("EUI-10"));
    }
}