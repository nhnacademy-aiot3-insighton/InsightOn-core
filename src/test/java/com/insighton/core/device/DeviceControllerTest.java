package com.insighton.core.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.device.dto.DeviceRequestDto;
import com.insighton.core.device.dto.DeviceUpdateRequest;
import com.insighton.core.device.entity.DeviceEntity;
import com.insighton.core.device.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper; // JSON 변환기
    @Autowired DeviceRepository deviceRepository;

    @BeforeEach
    void clean() {
        // 매 테스트가 실행되기 전에 DB를 깨끗하게 비웁니다.
        deviceRepository.deleteAll(); 
    }

    @Test
    @DisplayName("1. 장치 등록 성공")
    void create() throws Exception {
        DeviceRequestDto req = new DeviceRequestDto("센서", "SENSOR", "EUI-1", 100L, 1L); //[cite: 12]

        mockMvc.perform(post("/api/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()); // 200 상태코드 확인
    }

    @Test
    @DisplayName("2. 필수값 누락 시 에러 (400 Bad Request)")
    void create_fail() throws Exception {
        // 이름과 게이트웨이 ID를 비워서(@Valid 실패 유도) 요청[cite: 12]
        DeviceRequestDto req = new DeviceRequestDto("", "SENSOR", "EUI-2", null, 1L);

        mockMvc.perform(post("/api/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()); // 400 에러가 뜨면 통과!
    }

    @Test
    @DisplayName("3. 장치 검색 성공")
    void search() throws Exception {
        // Given: API를 거치지 않고 DB에 엔티티를 바로 꽂아 넣음 (테스트 속도 향상)[cite: 15]
        DeviceEntity saved = deviceRepository.save(
                DeviceEntity.builder().name("센서A").deviceEui("EUI-3").type("SENSOR").build()
        );

        // When & Then: EUI로 검색했을 때 정상 조회되는지 확인
        mockMvc.perform(get("/api/devices/search").param("eui", "EUI-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("센서A")); // 이름이 센서A인지 확인
    }

    @Test
    @DisplayName("4. 장치 위치 수정 성공")
    void update() throws Exception {
        // Given: 1층(1L)에 있는 기기를 DB에 미리 생성[cite: 15]
        DeviceEntity saved = deviceRepository.save(
                DeviceEntity.builder().name("센서B").deviceEui("EUI-4").type("SENSOR").locationsId(1L).build()
        );
        
        // 99층(99L)으로 바꾸겠다는 요청 DTO 생성[cite: 14]
        DeviceUpdateRequest req = new DeviceUpdateRequest("센서B", 99L);

        // When & Then: 수정 API(PATCH) 호출
        mockMvc.perform(patch("/api/devices/" + saved.getDeviceId() + "/location")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent()); // 204 상태코드 확인
    }
}