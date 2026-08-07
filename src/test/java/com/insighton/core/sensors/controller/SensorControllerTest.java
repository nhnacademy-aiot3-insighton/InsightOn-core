package com.insighton.core.sensors.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insighton.core.location.exception.LocationNotFoundException;
import com.insighton.core.sensors.dto.SensorResponse;
import com.insighton.core.sensors.dto.SensorUpdateRequest;
import com.insighton.core.sensors.exception.SensorNotFoundException;
import com.insighton.core.sensors.exception.InvalidSensorValueException;
import com.insighton.core.sensors.service.SensorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SensorController.class)
class SensorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SensorService sensorService;

    private SensorResponse sampleResponse() {
        return new SensorResponse(
                1L, 10L, 20L,
                "EUI-0001", "4층 CO2 센서",
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("단일 센서 조회 성공")
    void 센서_조회_성공() throws Exception {
        given(sensorService.getSensorById(eq(1L), eq(1L))).willReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorId").value(1L))
                .andExpect(jsonPath("$.sensorEui").value("EUI-0001"));
    }

    @Test
    @DisplayName("존재하지 않는 센서 조회 시 404")
    void 센서_조회_실패_404() throws Exception {
        given(sensorService.getSensorById(anyLong(), anyLong()))
                .willThrow(new SensorNotFoundException(999L));

        mockMvc.perform(get("/api/v1/sensor/{id}", 999L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("통합 검색은 groupId가 필수 쿼리 파라미터")
    void 검색_groupId_누락시_400() throws Exception {
        mockMvc.perform(get("/api/v1/sensor/search")
                        .header("X-USER-ID", 1L))
                .andExpect(status().isBadRequest());
    }

//    @Test
//    @DisplayName("장치 이름 수정 성공")
//    void 이름수정_성공() throws Exception {
//        SensorNameUpdateRequest request = new SensorNameUpdateRequest("새 이름");
//
//        mockMvc.perform(put("/api/v1/sensor/{id}/name", 1L)
//                        .header("X-USER-ID", 1L)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isNoContent());
//
//        verify(sensorService).updateSensorName(1L, 1L, "새 이름");
//    }

    @Test
    @DisplayName("위치만 수정 - 이름은 null로 그대로 전달됨")
    void 위치만_수정_성공() throws Exception {
        SensorUpdateRequest request = new SensorUpdateRequest(5L, null);

        mockMvc.perform(put("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(sensorService).updateSensor(1L, 1L, 5L, null);
    }

    @Test
    @DisplayName("이름만 수정 - 위치는 null로 그대로 전달됨")
    void 이름만_수정_성공() throws Exception {
        SensorUpdateRequest request = new SensorUpdateRequest(null, "새 이름");

        mockMvc.perform(put("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(sensorService).updateSensor(1L, 1L, null, "새 이름");
    }

    @Test
    @DisplayName("위치와 이름 둘 다 수정 성공")
    void 위치_이름_둘다_수정_성공() throws Exception {
        SensorUpdateRequest request = new SensorUpdateRequest(5L, "새 이름");

        mockMvc.perform(put("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(sensorService).updateSensor(1L, 1L, 5L, "새 이름");
    }

    @Test
    @DisplayName("이름이 100자를 넘으면 @Valid에 걸려 400, 서비스는 호출 안 됨")
    void 이름_길이초과_400() throws Exception {
        String tooLong = "가".repeat(101);
        SensorUpdateRequest request = new SensorUpdateRequest(null, tooLong);

        mockMvc.perform(put("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(sensorService, org.mockito.Mockito.never())
                .updateSensor(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("빈 문자열 이름은 @Valid는 통과하지만 서비스 레이어에서 400")
    void 빈이름_서비스레이어에서_거부() throws Exception {
        SensorUpdateRequest request = new SensorUpdateRequest(null, "   ");

        willThrow(new InvalidSensorValueException("변경할 장치 이름은 빈 값일 수 없습니다."))
                .given(sensorService).updateSensor(anyLong(), anyLong(), any(), anyString());

        mockMvc.perform(put("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 위치면 404")
    void 없는_위치_404() throws Exception {
        SensorUpdateRequest request = new SensorUpdateRequest(999L, null);

        willThrow(LocationNotFoundException.notFoundLocationByLocationId(999L))
                .given(sensorService).updateSensor(anyLong(), anyLong(), any(), any());

        mockMvc.perform(put("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("단일 센서 삭제 성공")
    void 센서_삭제_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/sensor/{id}", 1L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNoContent());

        verify(sensorService).deleteSensor(1L, 1L);
    }

    @Test
    @DisplayName("그룹 전체 센서 삭제 성공")
    void 전체삭제_성공() throws Exception {
        mockMvc.perform(delete("/api/v1/sensor")
                        .header("X-USER-ID", 1L)
                        .param("groupId", "5"))
                .andExpect(status().isNoContent());

        verify(sensorService).deleteAll(1L, 5L);
    }
}