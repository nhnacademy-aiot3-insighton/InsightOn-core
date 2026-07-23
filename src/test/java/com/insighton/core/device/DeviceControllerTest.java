package com.insighton.core.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.dto.DeviceRequestDto;
import com.insighton.core.dto.DeviceUpdateRequest;
import com.insighton.core.entity.DeviceEntity;
import com.insighton.core.entity.DeviceType;
import com.insighton.core.repository.DeviceRepository;
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
    ObjectMapper objectMapper; // JSON 변환기
    @Autowired
    DeviceRepository deviceRepository;

    @BeforeEach
    void clean() {
        // 매 테스트가 실행되기 전에 DB를 깨끗하게 비웁니다.
        deviceRepository.deleteAll();
    }

    @Test
    @DisplayName("1. 장치 등록 성공")
    void create() throws Exception {
        // 💡 name -> deviceName, "SENSOR" -> DeviceType.SENSOR 로 변경[cite: 44]
        DeviceRequestDto req = new DeviceRequestDto("센서", DeviceType.SENSOR, "EUI-1", 100L, 1L);

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()); // 200 상태코드 확인
    }

    @Test
    @DisplayName("2. 필수값 누락 시 에러 (400 Bad Request)")
    void create_fail() throws Exception {
        // 이름과 게이트웨이 ID를 비워서(@Valid 실패 유도) 요청[cite: 44]
        DeviceRequestDto req = new DeviceRequestDto("", DeviceType.SENSOR, "EUI-2", null, 1L);

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()); // 400 에러 확인
    }

    @Test
    @DisplayName("3. 장치 검색 성공")
    void search() throws Exception {
        // Given: DB에 엔티티를 미리 생성[cite: 44]
        DeviceEntity saved = deviceRepository.save(
                DeviceEntity.builder().deviceName("센서A").deviceEui("EUI-3").type(DeviceType.SENSOR).build()
        );

        // When & Then: EUI로 검색했을 때 정상 조회되는지 확인
        mockMvc.perform(get("/api/devices/search").param("eui", "EUI-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceName").value("센서A")); // 💡 jsonPath 속성명 변경[cite: 44]
    }

    @Test
    @DisplayName("4. 장치 위치 수정 성공")
    void update() throws Exception {
        // Given: 1층(1L)에 있는 기기를 DB에 미리 생성[cite: 44]
        DeviceEntity saved = deviceRepository.save(
                DeviceEntity.builder().deviceName("센서B").deviceEui("EUI-4").type(DeviceType.SENSOR).locationsId(1L).build()
        );

        // 99층(99L)으로 바꾸겠다는 요청 DTO 생성[cite: 44]
        DeviceUpdateRequest req = new DeviceUpdateRequest("센서B", 99L);

        // When & Then: 수정 API(PATCH) 호출
        mockMvc.perform(patch("/api/devices/" + saved.getDeviceId() + "/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent()); // 204 상태코드 확인
    }

    @Test
    @DisplayName("5. 단일 기기 조회 성공")
    void getDevice() throws Exception {
        // Given: DB에 기기 하나 미리 저장[cite: 44]
        DeviceEntity saved = deviceRepository.save(
                DeviceEntity.builder().deviceName("센서C").deviceEui("EUI-5").type(DeviceType.SENSOR).build()
        );

        // When & Then: 저장된 ID로 조회했을 때 이름이 일치하는지 확인[cite: 44]
        mockMvc.perform(get("/api/devices/" + saved.getDeviceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceName").value("센서C")); // 💡 jsonPath 속성명 변경[cite: 44]
    }

    @Test
    @DisplayName("6. 존재하지 않는 기기 조회 시 404")
    void getDevice_notFound() throws Exception {
        // Given: DB를 비운 상태이므로 999L은 존재하지 않는 ID[cite: 44]
        mockMvc.perform(get("/api/devices/999"))
                .andExpect(status().isNotFound()); // 404 에러 확인
    }

    @Test
    @DisplayName("7. 장치 삭제 성공")
    void deleteDevice() throws Exception {
        DeviceEntity saved = deviceRepository.save(
                DeviceEntity.builder().deviceName("센서D").deviceEui("EUI-6").type(DeviceType.SENSOR).build()
        );

        mockMvc.perform(delete("/api/devices/" + saved.getDeviceId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("8. 존재하지 않는 기기 삭제 시 404")
    void deleteDevice_notFound() throws Exception {
        mockMvc.perform(delete("/api/devices/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("9. 전체 삭제 성공")
    void deleteAllDevices() throws Exception {
        deviceRepository.save(DeviceEntity.builder().deviceName("센서E").deviceEui("EUI-7").type(DeviceType.SENSOR).build());
        deviceRepository.save(DeviceEntity.builder().deviceName("센서F").deviceEui("EUI-8").type(DeviceType.SENSOR).build());

        mockMvc.perform(delete("/api/devices"))
                .andExpect(status().isNoContent());

        assertThat(deviceRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("10. 게이트웨이 ID로 검색 성공")
    void search_byGatewayId() throws Exception {
        // Given[cite: 44]
        deviceRepository.save(
                DeviceEntity.builder().deviceName("센서G").deviceEui("EUI-9").type(DeviceType.SENSOR).gatewaysId(200L).build()
        );

        // When & Then[cite: 44]
        mockMvc.perform(get("/api/devices/search").param("gatewayId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceName").value("센서G")); // 💡 jsonPath 속성명 변경[cite: 44]
    }

    @Test
    @DisplayName("11. 이름으로 검색 성공")
    void search_byName() throws Exception {
        // Given[cite: 44]
        deviceRepository.save(
                DeviceEntity.builder().deviceName("센서H").deviceEui("EUI-10").type(DeviceType.SENSOR).build()
        );

        // When & Then: 💡 쿼리 파라미터 키를 deviceName으로 변경[cite: 44]
        mockMvc.perform(get("/api/devices/search").param("deviceName", "센서H"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceEui").value("EUI-10"));
    }
}